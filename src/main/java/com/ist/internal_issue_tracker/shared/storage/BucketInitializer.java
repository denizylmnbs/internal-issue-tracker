package com.ist.internal_issue_tracker.shared.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Best-effort convenience for local/dev: creates the configured bucket at startup if it does not
 * already exist. Guarded by {@code app.storage.auto-create-bucket} because in a real deployment
 * the bucket is provisioned by ops and this app's credentials will not carry {@code
 * s3:CreateBucket} - a hard-coded create attempt would log a scary {@code AccessDenied} on every
 * boot in that environment.
 *
 * <p>Every {@link SdkException} here is caught and logged, never rethrown: an exception escaping
 * {@link ApplicationRunner#run} aborts application startup entirely, and a slow-to-start or
 * temporarily unreachable MinIO must not be able to take the whole app down with it (see {@code
 * compose.yaml} - the {@code minio} service has no healthcheck, so Boot only waits for it to be
 * running, not ready; this runner can genuinely fire before MinIO is listening). {@link
 * S3ObjectStorage#put} has its own create-and-retry as the backstop for exactly that race.
 */
@Component
@Slf4j
class BucketInitializer implements ApplicationRunner {

  private final S3Client s3Client;
  private final String bucket;
  private final boolean autoCreateBucket;

  BucketInitializer(
      S3Client s3Client,
      @Value("${app.storage.bucket}") String bucket,
      @Value("${app.storage.auto-create-bucket:true}") boolean autoCreateBucket) {
    this.s3Client = s3Client;
    this.bucket = bucket;
    this.autoCreateBucket = autoCreateBucket;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!autoCreateBucket) {
      return;
    }
    try {
      s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
    } catch (NoSuchBucketException e) {
      try {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        log.info("Created object storage bucket {}", bucket);
      } catch (SdkException createFailure) {
        log.warn(
            "Could not create object storage bucket {}; avatars will fail until it exists",
            bucket,
            createFailure);
      }
    } catch (SdkException e) {
      log.warn(
          "Object storage unreachable at startup; avatars will fail until it returns", e);
    }
  }
}
