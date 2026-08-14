package com.ist.internal_issue_tracker.shared.storage;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * MinIO speaks the S3 API, so the official AWS SDK works against it unmodified once pointed at
 * MinIO's endpoint - no MinIO-specific SDK involved.
 *
 * <p>Two clients, two different endpoints, and that split is not incidental: {@link S3Client}
 * issues requests from <em>this backend</em>, so it is built against {@code app.storage.endpoint}
 * - reachable from wherever this process runs (e.g. {@code http://minio:9000} inside compose).
 * {@link S3Presigner} signs URLs the <em>browser</em> will fetch, so it is built against {@code
 * app.storage.public-endpoint} - reachable from wherever the browser runs (e.g. {@code
 * http://localhost:9000}). The signed host is part of the SigV4 signature, so a URL signed for
 * one cannot be rewritten to the other after the fact; when the two endpoints coincide (a single
 * local dev box) {@code app.storage.public-endpoint} simply defaults to {@code
 * app.storage.endpoint}.
 *
 * <p>{@code forcePathStyle(true)} is not optional: MinIO does not support virtual-hosted-style
 * addressing ({@code bucket.endpoint/key}), only path-style ({@code endpoint/bucket/key}). Without
 * it the SDK tries to resolve a hostname like {@code issue-tracker.localhost} and fails.
 *
 * <p>Neither builder performs network I/O, so a MinIO that is down or not yet ready cannot fail
 * bean creation - only actual requests (issued later, from {@link S3ObjectStorage} or {@link
 * S3UrlSigner}) can fail. See {@link BucketInitializer} for how the one operation that does need
 * MinIO to be up at startup (creating the bucket) tolerates it not being.
 */
@Configuration
@Slf4j
public class StorageConfig {

  /**
   * SigV4 signs a request but does not encrypt it, so over plain HTTP both the uploaded object and
   * the presigned URL's query-string signature cross the network in the clear. That is fine for a
   * local compose stack and never fine in production - hence a warning rather than a failure, since
   * refusing to start would break every developer's machine to guard against a deployment mistake.
   */
  private static void warnIfPlaintext(String label, String endpoint) {
    if (!URI.create(endpoint).getScheme().equalsIgnoreCase("https")) {
      log.warn(
          "Object storage {} {} is plaintext HTTP - object bodies and presigned URLs are"
              + " unencrypted in transit. Use https:// outside local development.",
          label,
          endpoint);
    }
  }

  @Bean(destroyMethod = "close")
  public S3Client s3Client(
      @Value("${app.storage.endpoint}") String endpoint,
      @Value("${app.storage.region}") String region,
      @Value("${app.storage.access-key}") String accessKey,
      @Value("${app.storage.secret-key}") String secretKey) {
    warnIfPlaintext("endpoint", endpoint);
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .forcePathStyle(true)
        .httpClientBuilder(UrlConnectionHttpClient.builder())
        .build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner(
      @Value("${app.storage.public-endpoint}") String publicEndpoint,
      @Value("${app.storage.region}") String region,
      @Value("${app.storage.access-key}") String accessKey,
      @Value("${app.storage.secret-key}") String secretKey) {
    warnIfPlaintext("public-endpoint", publicEndpoint);
    return S3Presigner.builder()
        .endpointOverride(URI.create(publicEndpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }
}
