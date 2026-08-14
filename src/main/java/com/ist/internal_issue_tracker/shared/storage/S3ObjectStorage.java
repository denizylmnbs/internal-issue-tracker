package com.ist.internal_issue_tracker.shared.storage;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Package-private: callers inject {@link ObjectStorage}, never this type directly, so nothing
 * outside this package needs to know an S3-flavored client is behind it.
 *
 * <p>Generated keys are {@code <prefix>/<uuid>} - the UUID means a replacement always produces a
 * brand new key rather than overwriting the old one, which is free cache-busting (no stale-URL
 * risk from a previous presigned link) and avoids any read-your-writes race against MinIO. No
 * file extension is appended: the object's {@code Content-Type} is stored as metadata and MinIO
 * echoes it on every GET, so nothing downstream needs the key itself to carry format info.
 * {@code contentDisposition("inline")} tells browsers to render the object rather than download
 * it, which is what a profile-picture {@code <img src>} needs.
 */
@Component
@Slf4j
class S3ObjectStorage implements ObjectStorage {

  private final S3Client s3Client;
  private final String bucket;

  S3ObjectStorage(S3Client s3Client, @Value("${app.storage.bucket}") String bucket) {
    this.s3Client = s3Client;
    this.bucket = bucket;
  }

  @Override
  public String put(String keyPrefix, byte[] content, String contentType) {
    String key = keyPrefix + "/" + UUID.randomUUID();
    putObject(key, content, contentType);
    return key;
  }

  private void putObject(String key, byte[] content, String contentType) {
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentDisposition("inline")
            .build();
    try {
      s3Client.putObject(request, RequestBody.fromBytes(content));
    } catch (NoSuchBucketException e) {
      // spring-boot-docker-compose starts the minio container but does not wait for it to be
      // ready (compose.yaml has no healthcheck for it), so the bucket BucketInitializer tries to
      // create at startup may not exist yet by the time the first upload arrives. One
      // create-and-retry lets that race self-heal instead of requiring a restart.
      log.warn("Bucket {} missing on first write, creating and retrying once", bucket);
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
      try {
        s3Client.putObject(request, RequestBody.fromBytes(content));
      } catch (SdkException retryFailure) {
        throw new ObjectStorageException("Failed to store object " + key, retryFailure);
      }
    } catch (SdkException e) {
      throw new ObjectStorageException("Failed to store object " + key, e);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    } catch (SdkException e) {
      // S3's DeleteObject is already idempotent server-side (deleting a missing key is not an
      // error), so anything reaching this catch is a genuine infrastructure failure - propagate
      // it rather than swallow it here; callers that want "best effort" wrap this call themselves
      // (see UserAvatarService, which deliberately does exactly that around a stale-key delete).
      throw new ObjectStorageException("Failed to delete object " + objectKey, e);
    }
  }
}
