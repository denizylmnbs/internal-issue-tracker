package com.ist.internal_issue_tracker.shared.storage;

/**
 * Thrown by {@link ImageNormalizer} when the uploaded bytes cannot be turned into a safe image.
 *
 * <p>Carries a {@link Reason} rather than an HTTP status, for the same reason {@link
 * ImageContentTypeDetector} reports what bytes <em>are</em> instead of what is <em>acceptable</em>:
 * the mapping from "unreadable" to a status code is the calling feature's policy, not this layer's.
 * {@code UserAvatarService} is where that mapping lives.
 *
 * <p>Unlike {@link ObjectStorageException} this is <em>not</em> an infrastructure failure - it is
 * always caused by the bytes the caller supplied, so it must reach the client as a 4xx rather than
 * disappearing into the trace-id catch-all.
 */
public class ImageProcessingException extends RuntimeException {

  /**
   * {@code TOO_LARGE} means the image's declared pixel dimensions exceed the configured guard - it
   * says nothing about the encoded byte length, which is capped separately and much earlier. A 2MB
   * PNG that decodes to 30000x30000 is exactly the case this exists for.
   */
  public enum Reason {
    UNREADABLE,
    TOO_LARGE
  }

  private final Reason reason;

  public ImageProcessingException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public ImageProcessingException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
