package com.fkmed.infra.storage;

import com.fkmed.domain.upload.FileStorageException;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Amazon S3 adapter using the SDK default credential provider chain. */
@Component
final class S3StorageAdapter implements StorageBackendAdapter {

  private final StorageProperties.S3Settings settings;
  private final Supplier<S3Client> clientFactory;
  private volatile S3Client client;

  @Autowired
  S3StorageAdapter(StorageProperties properties) {
    this(properties.s3(), () -> buildClient(properties.s3()));
  }

  S3StorageAdapter(StorageProperties.S3Settings settings, Supplier<S3Client> clientFactory) {
    this.settings = settings;
    this.clientFactory = clientFactory;
  }

  @Override
  public StorageBackendType type() {
    return StorageBackendType.S3;
  }

  @Override
  public void put(String key, byte[] content) {
    requireConfigured();
    try {
      PutObjectRequest.Builder request =
          PutObjectRequest.builder()
              .bucket(settings.bucket())
              .key(objectKey(key))
              .contentLength((long) content.length);
      if (settings.kmsKeyId().isBlank()) {
        request.serverSideEncryption(ServerSideEncryption.AES256);
      } else {
        request.serverSideEncryption(ServerSideEncryption.AWS_KMS).ssekmsKeyId(settings.kmsKeyId());
      }
      client().putObject(request.build(), RequestBody.fromBytes(content));
    } catch (SdkException exception) {
      throw new FileStorageException("could not store S3 content", exception);
    }
  }

  @Override
  public byte[] get(String key) {
    requireConfigured();
    try {
      return client()
          .getObjectAsBytes(
              GetObjectRequest.builder().bucket(settings.bucket()).key(objectKey(key)).build())
          .asByteArray();
    } catch (SdkException exception) {
      throw new FileStorageException("could not read S3 content", exception);
    }
  }

  @Override
  public void delete(String key) {
    requireConfigured();
    try {
      client()
          .deleteObject(
              DeleteObjectRequest.builder().bucket(settings.bucket()).key(objectKey(key)).build());
    } catch (SdkException exception) {
      throw new FileStorageException("could not delete S3 content", exception);
    }
  }

  @PreDestroy
  void close() {
    S3Client existing = client;
    if (existing != null) {
      existing.close();
    }
  }

  private S3Client client() {
    S3Client existing = client;
    if (existing != null) {
      return existing;
    }
    synchronized (this) {
      if (client == null) {
        client = clientFactory.get();
      }
      return client;
    }
  }

  private String objectKey(String key) {
    return settings.prefix().isBlank() ? key : settings.prefix() + "/" + key;
  }

  private void requireConfigured() {
    if (settings.bucket().isBlank() || settings.region().isBlank()) {
      throw new FileStorageException("S3 storage requires bucket and region");
    }
  }

  private static S3Client buildClient(StorageProperties.S3Settings settings) {
    var builder =
        S3Client.builder()
            .region(Region.of(settings.region()))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .apiCallAttemptTimeout(Duration.ofSeconds(10))
                    .apiCallTimeout(Duration.ofSeconds(30))
                    .build())
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(settings.pathStyle()).build());
    if (!settings.endpoint().isBlank()) {
      builder.endpointOverride(URI.create(settings.endpoint()));
    }
    return builder.build();
  }
}
