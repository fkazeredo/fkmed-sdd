package com.fkmed.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** SPEC-0019: environment defaults stay synchronized across Spring and Compose. */
class StorageConfigurationTest {

  @Test
  void springProfilesSelectPostgresFilesystemAndS3AsDesigned() throws Exception {
    String base = read("src/main/resources/application.yaml");
    String dev = read("src/main/resources/application-dev.yaml");
    String prod = read("src/main/resources/application-prod.yaml");

    assertThat(base).contains("backend: ${FKMED_STORAGE_BACKEND:postgres}");
    assertThat(dev).contains("backend: ${FKMED_STORAGE_BACKEND:filesystem}");
    assertThat(prod).contains("backend: ${FKMED_STORAGE_BACKEND:s3}");
  }

  @Test
  void composeStacksProvisionTheExpectedStorageBackend() throws Exception {
    String devCompose = read("../docker-compose.yml");
    String e2eCompose = read("../compose.e2e.yaml");
    String prodCompose = read("../compose.prod.yaml");

    assertThat(devCompose)
        .contains("FKMED_STORAGE_BACKEND: ${FKMED_STORAGE_BACKEND:-filesystem}")
        .contains("file-storage:/fkmed");
    assertThat(e2eCompose).contains("FKMED_STORAGE_BACKEND: filesystem").contains("- /fkmed");
    assertThat(prodCompose)
        .contains("FKMED_STORAGE_BACKEND: ${FKMED_STORAGE_BACKEND:-s3}")
        .contains(
            "FKMED_STORAGE_S3_BUCKET: ${FKMED_STORAGE_S3_BUCKET:?private S3 bucket required}");
  }

  private static String read(String path) throws Exception {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }
}
