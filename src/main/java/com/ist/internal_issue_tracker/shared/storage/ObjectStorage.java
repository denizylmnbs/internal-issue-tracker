package com.ist.internal_issue_tracker.shared.storage;

/**
 * Write side of object storage: put bytes in, get an opaque key back. Any module may inject this
 * directly - {@code shared} is open, and this concern owns no table of its own, so there is no
 * port/adapter split the way {@code shared.port} uses for module-trapped implementations.
 *
 * <p>{@code byte[]} rather than {@code InputStream} is a deliberate bound tied to the current
 * caller (a 2MB avatar, already fully buffered for the magic-byte check before this is called).
 * Add an {@code InputStream}-based overload when a caller needs to stream something larger.
 */
public interface ObjectStorage {

  /**
   * Stores {@code content} under a key generated from {@code keyPrefix} (e.g. {@code
   * "avatars/42"}) and returns that key. Callers never construct keys themselves - the generated
   * suffix is what gives every write a fresh identity, so a replacement is a new object rather
   * than an overwrite of the old one.
   */
  String put(String keyPrefix, byte[] content, String contentType);

  /**
   * Idempotent: deleting a key that does not exist (already removed, or never existed) is not an
   * error. Callers that need "delete the previous object" logic can call this with a stale key
   * without checking existence first.
   */
  void delete(String objectKey);
}
