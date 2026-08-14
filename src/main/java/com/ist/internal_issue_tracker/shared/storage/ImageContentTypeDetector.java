package com.ist.internal_issue_tracker.shared.storage;

import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Identifies an image format from its leading bytes ("magic numbers"), never from a client-
 * supplied filename or declared {@code Content-Type} - both are attacker-controlled and are
 * discarded by every caller of this class rather than compared against the sniffed result. That
 * is what makes content-type spoofing structurally impossible for callers that use this: the
 * value stored is the value sniffed, full stop.
 *
 * <p>Deliberately policy-free: this reports what the bytes <em>are</em>, not what is
 * <em>acceptable</em>. Which formats a given feature allows (e.g. avatars excluding SVG to avoid
 * an XSS vector on the presigned-URL origin) is that feature's decision, made against this
 * class's output - kept separate so a wider allow-list elsewhere (issue attachments, say) can
 * reuse the same sniffing without inheriting avatar-specific policy.
 */
@Component
public class ImageContentTypeDetector {

  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
  private static final byte[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50}; // "WEBP"
  private static final int WEBP_SIGNATURE_OFFSET = 8;

  /**
   * Empty when the content is too short to carry a recognized signature or matches none of them -
   * callers should treat that the same as "unsupported", not throw. Guards array length before
   * every offset read, so a handful of bytes cannot throw {@link ArrayIndexOutOfBoundsException}.
   */
  public Optional<String> detect(byte[] content) {
    if (matches(content, PNG_SIGNATURE, 0)) {
      return Optional.of("image/png");
    }
    if (matches(content, JPEG_SIGNATURE, 0)) {
      return Optional.of("image/jpeg");
    }
    if (matches(content, RIFF_SIGNATURE, 0)
        && matches(content, WEBP_SIGNATURE, WEBP_SIGNATURE_OFFSET)) {
      return Optional.of("image/webp");
    }
    return Optional.empty();
  }

  private static boolean matches(byte[] content, byte[] signature, int offset) {
    if (content.length < offset + signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if (content[offset + i] != signature[i]) {
        return false;
      }
    }
    return true;
  }
}
