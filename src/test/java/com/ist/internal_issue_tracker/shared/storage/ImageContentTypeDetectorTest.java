package com.ist.internal_issue_tracker.shared.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * No mocks and no context: the detector is a pure function of bytes to an {@link Optional}
 * content type.
 */
class ImageContentTypeDetectorTest {

  private final ImageContentTypeDetector detector = new ImageContentTypeDetector();

  private static byte[] concat(byte[]... parts) {
    int length = 0;
    for (byte[] part : parts) {
      length += part.length;
    }
    byte[] result = new byte[length];
    int offset = 0;
    for (byte[] part : parts) {
      System.arraycopy(part, 0, result, offset, part.length);
      offset += part.length;
    }
    return result;
  }

  @Test
  void detect_returnsPng_forPngSignature() {
    byte[] png = {
      (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    assertThat(detector.detect(png)).contains("image/png");
  }

  @Test
  void detect_returnsPng_regardlessOfDeclaredFilenameExtension() {
    // The signature is all that matters - callers never pass a filename in here, and this proves
    // why: an attacker-controlled ".jpg" name on PNG bytes still detects as image/png.
    byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    assertThat(detector.detect(png)).contains("image/png");
  }

  @Test
  void detect_returnsJpeg_forJpegSignature() {
    byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    assertThat(detector.detect(jpeg)).contains("image/jpeg");
  }

  @Test
  void detect_returnsWebp_forRiffWebpSignature() {
    byte[] riff = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00};
    byte[] webp = {0x57, 0x45, 0x42, 0x50};

    assertThat(detector.detect(concat(riff, webp))).contains("image/webp");
  }

  @Test
  void detect_returnsEmpty_forRiffContainerThatIsNotWebp() {
    byte[] riff = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00};
    byte[] notWebp = {0x41, 0x56, 0x49, 0x20}; // "AVI "

    assertThat(detector.detect(concat(riff, notWebp))).isEmpty();
  }

  @Test
  void detect_returnsEmpty_forPlainText() {
    assertThat(detector.detect("not an image".getBytes())).isEmpty();
  }

  @Test
  void detect_returnsEmpty_forContentShorterThanAnySignature() {
    // Regression guard: this must not throw ArrayIndexOutOfBoundsException.
    byte[] tooShort = {0x01, 0x02};

    assertThat(detector.detect(tooShort)).isEmpty();
  }

  @Test
  void detect_returnsEmpty_forEmptyContent() {
    assertThat(detector.detect(new byte[0])).isEmpty();
  }
}
