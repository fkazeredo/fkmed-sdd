package com.fkmed.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.upload.FileStorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

class S3StorageAdapterTest {

  private static final String KEY = "reimbursement-document/12345678-1234-4234-8234-123456789abc";

  @Test
  void springContextSelectsTheProductionConstructor() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(
          StorageProperties.class,
          () ->
              new StorageProperties(
                  StorageBackendType.POSTGRES,
                  new StorageProperties.Filesystem("/tmp/fkmed"),
                  new StorageProperties.S3Settings("", "", "", "", "", false)));
      context.register(S3StorageAdapter.class);

      context.refresh();

      assertThat(context.getBean(S3StorageAdapter.class).type()).isEqualTo(StorageBackendType.S3);
    }
  }

  @Test
  void roundTripUsesConfiguredBucketPrefixAndSseS3() {
    S3Client client = mock(S3Client.class);
    when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    when(client.getObjectAsBytes(any(GetObjectRequest.class)))
        .thenReturn(
            ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[] {1, 2, 3}));
    S3StorageAdapter adapter = adapter(client, "");

    adapter.put(KEY, new byte[] {1, 2, 3});
    byte[] result = adapter.get(KEY);
    adapter.delete(KEY);

    ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(put.capture(), any(RequestBody.class));
    assertThat(put.getValue().bucket()).isEqualTo("private-fkmed");
    assertThat(put.getValue().key()).isEqualTo("tenant-a/" + KEY);
    assertThat(put.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
    assertThat(result).containsExactly(1, 2, 3);
    verify(client)
        .deleteObject(
            DeleteObjectRequest.builder().bucket("private-fkmed").key("tenant-a/" + KEY).build());
  }

  @Test
  void configuredKmsKeySelectsSseKms() {
    S3Client client = mock(S3Client.class);
    when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    S3StorageAdapter adapter = adapter(client, "alias/fkmed");

    adapter.put(KEY, new byte[] {1});

    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(request.capture(), any(RequestBody.class));
    assertThat(request.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AWS_KMS);
    assertThat(request.getValue().ssekmsKeyId()).isEqualTo("alias/fkmed");
  }

  @Test
  void missingBucketOrRegionFailsBeforeCreatingClient() {
    S3StorageAdapter adapter =
        new S3StorageAdapter(
            new StorageProperties.S3Settings("", "", "", "", "", false),
            () -> {
              throw new AssertionError("client must not be created");
            });

    assertThatThrownBy(() -> adapter.put(KEY, new byte[] {1}))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("bucket and region");
  }

  private static S3StorageAdapter adapter(S3Client client, String kmsKey) {
    return new S3StorageAdapter(
        new StorageProperties.S3Settings(
            "private-fkmed", "sa-east-1", "tenant-a", kmsKey, "", false),
        () -> client);
  }
}
