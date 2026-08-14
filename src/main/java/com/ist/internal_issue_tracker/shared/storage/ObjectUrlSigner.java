package com.ist.internal_issue_tracker.shared.storage;

import java.time.Duration;

/**
 * Read side of object storage: turns an object key into a URL the <em>browser</em> can fetch
 * directly, bypassing the backend entirely. Deliberately a separate interface from {@link
 * ObjectStorage} rather than one three-method type - the two need different endpoints. {@link
 * ObjectStorage} talks to the endpoint this backend can reach; this signs for the endpoint the
 * browser can reach. The host is part of the SigV4 signature, so it cannot be rewritten after
 * signing - see {@code StorageConfig} for how the two endpoints are configured.
 *
 * <p>Presigning is local HMAC-SHA256 computation, not a network call - cheap enough to call once
 * per row in a list response. The returned URL is time-limited by construction: never persist it,
 * never cache it beyond its own TTL, and never assume a URL handed out five minutes ago is still
 * valid.
 */
public interface ObjectUrlSigner {

  /** Signs with the configured default TTL ({@code app.storage.presigned-url-ttl}). */
  String presignedGetUrl(String objectKey);

  String presignedGetUrl(String objectKey, Duration ttl);
}
