package com.fkmed.infra.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

class StorageProductionValidatorTest {

  @Test
  void rejectsNonS3OrIncompleteProductionConfiguration() {
    StorageProperties properties =
        new StorageProperties(
            StorageBackendType.FILESYSTEM,
            new StorageProperties.Filesystem("/fkmed/uploads"),
            new StorageProperties.S3Settings("", "", "fkmed", "", "", false));

    assertThatIllegalStateException()
        .isThrownBy(() -> new StorageProductionValidator(properties).run(null))
        .withMessageContaining("backend must be s3")
        .withMessageContaining("bucket")
        .withMessageContaining("region");
  }

  @Test
  void acceptsCompleteS3ProductionConfiguration() {
    StorageProperties properties =
        new StorageProperties(
            StorageBackendType.S3,
            new StorageProperties.Filesystem("/fkmed/uploads"),
            new StorageProperties.S3Settings("private-fkmed", "sa-east-1", "fkmed", "", "", false));

    assertThatCode(() -> new StorageProductionValidator(properties).run(null))
        .doesNotThrowAnyException();
  }
}
