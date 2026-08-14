package com.ist.internal_issue_tracker.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ist.internal_issue_tracker.shared.storage.ImageNormalizer.NormalizedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * No Spring context: {@link ImageNormalizer} takes its guards as constructor arguments precisely so
 * they can be dialed down here, which is what lets the oversize case be tested with a 40x40 image
 * instead of a fixture large enough to trip a production-sized limit.
 */
class ImageNormalizerTest {

  private static final int TARGET = 512;

  private final ImageNormalizer normalizer = new ImageNormalizer(4096, 16_777_216L);

  private static byte[] image(String format, int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.CYAN);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, format, out);
    return out.toByteArray();
  }

  private static BufferedImage decode(byte[] content) throws IOException {
    return ImageIO.read(new ByteArrayInputStream(content));
  }

  private static boolean contains(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  @Test
  void normalize_alwaysProducesDecodablePng_evenFromJpegInput() throws IOException {
    NormalizedImage result = normalizer.normalize(image("jpg", 200, 200), TARGET);

    assertThat(result.contentType()).isEqualTo("image/png");
    assertThat(new ImageContentTypeDetector().detect(result.content())).contains("image/png");
    assertThat(decode(result.content())).isNotNull();
  }

  @Test
  void normalize_scalesDownPreservingAspectRatio() throws IOException {
    NormalizedImage result = normalizer.normalize(image("png", 1000, 400), TARGET);

    BufferedImage decoded = decode(result.content());
    assertThat(decoded.getWidth()).isEqualTo(512);
    assertThat(decoded.getHeight()).isEqualTo(205);
  }

  @Test
  void normalize_neverUpscalesASmallImage() throws IOException {
    NormalizedImage result = normalizer.normalize(image("png", 100, 100), TARGET);

    BufferedImage decoded = decode(result.content());
    assertThat(decoded.getWidth()).isEqualTo(100);
    assertThat(decoded.getHeight()).isEqualTo(100);
  }

  @Test
  void normalize_dropsBytesAppendedAfterAValidImage() throws IOException {
    // The whole reason this class exists. `cat valid.png payload > avatar.png` sniffs as image/png
    // and used to be stored verbatim; the appended bytes are not pixels, so they must not survive
    // into the re-encoded output.
    byte[] payload = "MZ-EXECUTABLE-PAYLOAD-MARKER".getBytes(StandardCharsets.US_ASCII);
    byte[] png = image("png", 64, 64);
    byte[] polyglot = Arrays.copyOf(png, png.length + payload.length);
    System.arraycopy(payload, 0, polyglot, png.length, payload.length);

    // Sanity check that the fixture really is the attack: the payload is present going in.
    assertThat(contains(polyglot, payload)).isTrue();

    NormalizedImage result = normalizer.normalize(polyglot, TARGET);

    assertThat(contains(result.content(), payload)).isFalse();
  }

  /**
   * Splices a minimal but well-formed APP1/Exif segment in right after the SOI marker - the same
   * place a camera puts one. The payload is an empty little-endian TIFF header, which is enough to
   * be a real segment without needing a real photo as a fixture.
   */
  private static byte[] withExifSegment(byte[] jpeg) {
    byte[] tiff = {
      'I', 'I', 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    byte[] header = {'E', 'x', 'i', 'f', 0x00, 0x00};
    int payloadLength = header.length + tiff.length + 2;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(jpeg[0]); // FF
    out.write(jpeg[1]); // D8 (SOI)
    out.write(0xFF);
    out.write(0xE1); // APP1
    out.write((payloadLength >> 8) & 0xFF);
    out.write(payloadLength & 0xFF);
    out.writeBytes(header);
    out.writeBytes(tiff);
    out.write(jpeg, 2, jpeg.length - 2);
    return out.toByteArray();
  }

  @Test
  void normalize_stripsExifMetadata() throws IOException {
    // A phone JPEG carries GPS coordinates and device identifiers in its APP1/Exif segment. The
    // output is drawn from decoded pixels onto a fresh raster, so no source segment can ride along.
    byte[] exifMarker = "Exif".getBytes(StandardCharsets.US_ASCII);
    byte[] jpeg = withExifSegment(image("jpg", 120, 120));

    // The fixture has to actually carry the segment, or this asserts nothing.
    assertThat(contains(jpeg, exifMarker)).isTrue();

    NormalizedImage result = normalizer.normalize(jpeg, TARGET);

    assertThat(contains(result.content(), exifMarker)).isFalse();
  }

  @Test
  void normalize_rejectsAValidHeaderFollowedByACorruptBody() throws IOException {
    // A prefix check passes this; only a full decode catches it.
    byte[] png = image("png", 64, 64);
    byte[] truncated = Arrays.copyOf(png, png.length / 2);

    assertThatThrownBy(() -> normalizer.normalize(truncated, TARGET))
        .isInstanceOf(ImageProcessingException.class)
        .extracting(ex -> ((ImageProcessingException) ex).reason())
        .isEqualTo(ImageProcessingException.Reason.UNREADABLE);
  }

  @Test
  void normalize_rejectsBytesThatAreNotAnImageAtAll() {
    byte[] text = "this is not an image".getBytes(StandardCharsets.US_ASCII);

    assertThatThrownBy(() -> normalizer.normalize(text, TARGET))
        .isInstanceOf(ImageProcessingException.class)
        .extracting(ex -> ((ImageProcessingException) ex).reason())
        .isEqualTo(ImageProcessingException.Reason.UNREADABLE);
  }

  @Test
  void normalize_rejectsAnImageExceedingTheDimensionGuard_beforeDecodingIt() throws IOException {
    ImageNormalizer strict = new ImageNormalizer(32, 16_777_216L);

    assertThatThrownBy(() -> strict.normalize(image("png", 40, 40), TARGET))
        .isInstanceOf(ImageProcessingException.class)
        .extracting(ex -> ((ImageProcessingException) ex).reason())
        .isEqualTo(ImageProcessingException.Reason.TOO_LARGE);
  }

  @Test
  void normalize_rejectsAnImageExceedingThePixelGuard() throws IOException {
    // Both edges are within max-dimension; only their product trips the guard. This is the shape
    // of a decompression bomb that a per-edge check alone would let through.
    ImageNormalizer strict = new ImageNormalizer(4096, 100L);

    assertThatThrownBy(() -> strict.normalize(image("png", 40, 40), TARGET))
        .isInstanceOf(ImageProcessingException.class)
        .extracting(ex -> ((ImageProcessingException) ex).reason())
        .isEqualTo(ImageProcessingException.Reason.TOO_LARGE);
  }
}
