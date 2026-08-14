package com.ist.internal_issue_tracker.shared.storage;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Package-private for the same reason as {@link S3ObjectStorage}: callers depend on {@link
 * ObjectUrlSigner}, not this class.
 *
 * <p>No caching here, and that omission is deliberate rather than an oversight: the output is
 * time-varying by construction (it embeds an expiry), so caching it would mean caching an expiry
 * date - the cached value would eventually be handed out already expired.
 */
@Component
class S3UrlSigner implements ObjectUrlSigner {

  private final S3Presigner s3Presigner;
  private final String bucket;
  private final Duration defaultTtl;

  S3UrlSigner(
      S3Presigner s3Presigner,
      @Value("${app.storage.bucket}") String bucket,
      @Value("${app.storage.presigned-url-ttl}") Duration defaultTtl) {
    this.s3Presigner = s3Presigner;
    this.bucket = bucket;
    this.defaultTtl = defaultTtl;
  }

  @Override
  public String presignedGetUrl(String objectKey) {
    return presignedGetUrl(objectKey, defaultTtl);
  }

  @Override
  public String presignedGetUrl(String objectKey, Duration ttl) {
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(ttl)
            .getObjectRequest(
                GetObjectRequest.builder().bucket(bucket).key(objectKey).build())
            .build();
    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
