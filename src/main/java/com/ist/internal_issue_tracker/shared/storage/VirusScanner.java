package com.ist.internal_issue_tracker.shared.storage;

/**
 * Scans uploaded bytes for malware before they are handed to {@link ObjectStorage}.
 *
 * <p>Reports a result rather than throwing, deliberately. What to do when the scanner cannot be
 * reached is a security <em>policy</em> decision - fail closed and reject the upload, or fail open
 * and accept it - and policy belongs with the feature, not the adapter. Keeping it here would bury
 * the single most important decision in this class inside an infrastructure component; keeping it
 * in {@code UserAvatarService} makes it visible and unit-testable without a scanner running.
 *
 * <p>When scanning is disabled there is no bean at all rather than a no-op implementation, so
 * callers inject {@code Optional<VirusScanner>} and an empty Optional means "disabled". A fourth
 * {@link Status} constant for that case would let a disabled scanner masquerade as a result.
 */
public interface VirusScanner {

  enum Status {
    CLEAN,
    INFECTED,
    /** The scanner was unreachable, timed out, or answered something unparseable. */
    UNAVAILABLE
  }

  /**
   * {@code signature} names the matched malware definition and is only populated for {@link
   * Status#INFECTED}. It is useful in logs and must not be echoed back to the client - it tells an
   * attacker exactly which definition tripped, which is the feedback loop for evading it.
   */
  record ScanResult(Status status, String signature) {

    public static ScanResult clean() {
      return new ScanResult(Status.CLEAN, null);
    }

    public static ScanResult infected(String signature) {
      return new ScanResult(Status.INFECTED, signature);
    }

    public static ScanResult unavailable() {
      return new ScanResult(Status.UNAVAILABLE, null);
    }
  }

  /** Never throws for an unreachable scanner - that is {@link Status#UNAVAILABLE}. */
  ScanResult scan(byte[] content);
}
