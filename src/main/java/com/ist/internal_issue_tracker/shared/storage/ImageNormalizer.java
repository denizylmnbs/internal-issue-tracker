package com.ist.internal_issue_tracker.shared.storage;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Decodes an uploaded image, redraws it into a fresh raster, and re-encodes it as PNG. The output
 * is never the input: it is a brand new image built pixel by pixel from the decoded source, which
 * is what makes this the single fix for three separate problems at once.
 *
 * <ul>
 *   <li><b>Appended payloads.</b> {@link ImageContentTypeDetector} only inspects the first few
 *       bytes, so {@code cat valid.png malware.exe > avatar.png} sniffs as {@code image/png} and,
 *       before this class existed, was stored verbatim. Here the trailing bytes are not part of any
 *       decoded pixel and simply do not survive into the re-encoded output.
 *   <li><b>Structurally invalid images.</b> A valid signature is no longer enough - {@link
 *       ImageReader#read(int)} has to make it all the way through the stream, so a truncated or
 *       corrupt body is rejected rather than stored.
 *   <li><b>Metadata.</b> EXIF (including GPS coordinates and device identifiers on phone photos) is
 *       source metadata; nothing copies it onto the new image, so it is gone by construction rather
 *       than by an explicit stripping step that could be forgotten for a new format.
 * </ul>
 *
 * <p><b>The dimension guard runs before decoding, and that ordering is the entire point.</b> A 2MB
 * PNG can legitimately declare 30000x30000 pixels; decoding it first to find out how big it is
 * would allocate gigabytes before any check could fire. {@link ImageReader#getWidth(int)} reads the
 * header only, so the rejection happens before a single pixel buffer exists.
 *
 * <p>Peak transient cost is roughly {@code maxPixels * 4} bytes (ARGB), so the default 16M-pixel
 * guard admits ~64MB allocations. What bounds the <em>concurrent</em> total is the upload-specific
 * rate limit in {@code RateLimiterService.perUpload()}; the two numbers are meant to be tuned
 * together, and lowering the guard is the cheaper lever if heap pressure ever shows up.
 *
 * <p>Deliberately not policy: the caps here are DoS guards that belong to this component, but the
 * target size is the calling feature's decision and is therefore a parameter.
 */
@Component
public class ImageNormalizer {

  static {
    // Without this ImageIO spills large rasters to temp files on disk. For a 2MB in-memory upload
    // that is pure downside: an unnecessary filesystem surface and slower processing.
    ImageIO.setUseCache(false);
  }

  /** Always PNG - a single stored format, and one that preserves the alpha channel. */
  private static final String OUTPUT_FORMAT = "png";

  private static final String OUTPUT_CONTENT_TYPE = "image/png";

  /**
   * The re-encoded result. {@code contentType} is always {@code image/png} regardless of what went
   * in; it is returned rather than assumed so callers pass it straight to {@link
   * ObjectStorage#put(String, byte[], String)} without re-deriving it.
   */
  public record NormalizedImage(byte[] content, String contentType) {}

  private final int maxDimension;
  private final long maxPixels;

  public ImageNormalizer(
      @Value("${app.storage.image.max-dimension}") int maxDimension,
      @Value("${app.storage.image.max-pixels}") long maxPixels) {
    this.maxDimension = maxDimension;
    this.maxPixels = maxPixels;
  }

  /**
   * @param targetSize longest edge of the output, in pixels; smaller images are never upscaled
   * @throws ImageProcessingException with {@code TOO_LARGE} when the source's declared dimensions
   *     exceed the configured guards, or {@code UNREADABLE} when no registered reader recognizes
   *     the bytes or decoding fails partway through
   */
  public NormalizedImage normalize(byte[] content, int targetSize) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
      if (input == null) {
        throw new ImageProcessingException(
            ImageProcessingException.Reason.UNREADABLE, "No image input stream for the uploaded bytes");
      }
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new ImageProcessingException(
            ImageProcessingException.Reason.UNREADABLE, "No registered reader recognizes the uploaded bytes");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        return normalize(reader, targetSize);
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new ImageProcessingException(
          ImageProcessingException.Reason.UNREADABLE, "Failed to read the uploaded image", e);
    }
  }

  private NormalizedImage normalize(ImageReader reader, int targetSize) throws IOException {
    requireSaneDimensions(reader);

    BufferedImage source;
    try {
      source = reader.read(0);
    } catch (IOException | RuntimeException e) {
      // The signature matched but the body did not survive a full decode - truncated, corrupt, or
      // a payload wearing an image header. Either way it is the caller's bytes that are wrong.
      throw new ImageProcessingException(
          ImageProcessingException.Reason.UNREADABLE, "The uploaded image could not be decoded", e);
    }

    BufferedImage normalized = redraw(source, targetSize);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(normalized, OUTPUT_FORMAT, out)) {
      throw new ImageProcessingException(
          ImageProcessingException.Reason.UNREADABLE, "No writer available for the normalized image");
    }
    return new NormalizedImage(out.toByteArray(), OUTPUT_CONTENT_TYPE);
  }

  /** Header-only read; must stay ahead of {@link ImageReader#read(int)}. See the class Javadoc. */
  private void requireSaneDimensions(ImageReader reader) throws IOException {
    int width = reader.getWidth(0);
    int height = reader.getHeight(0);
    if (width > maxDimension || height > maxDimension || (long) width * height > maxPixels) {
      throw new ImageProcessingException(
          ImageProcessingException.Reason.TOO_LARGE,
          "Image dimensions %dx%d exceed the allowed maximum".formatted(width, height));
    }
  }

  private static BufferedImage redraw(BufferedImage source, int targetSize) {
    int width = source.getWidth();
    int height = source.getHeight();
    // Never upscale: enlarging a 64x64 avatar to 512x512 costs bytes and adds no detail.
    double scale = Math.min(1.0, (double) targetSize / Math.max(width, height));
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));

    // TYPE_INT_ARGB unconditionally: a transparent PNG flattened onto an opaque background would
    // show white corners once the UI crops the avatar into a circle.
    BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(
          RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }
}
