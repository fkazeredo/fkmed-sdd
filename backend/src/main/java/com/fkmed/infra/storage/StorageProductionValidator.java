package com.fkmed.infra.storage;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Production fail-fast guard for the owner-selected private S3 storage posture. */
@Component
@Profile("prod")
@RequiredArgsConstructor
public final class StorageProductionValidator implements ApplicationRunner {

  private final StorageProperties properties;

  @Override
  public void run(ApplicationArguments args) {
    List<String> violations = new ArrayList<>();
    if (properties.backend() != StorageBackendType.S3) {
      violations.add("app.storage.backend must be s3 in prod");
    }
    if (properties.s3().bucket().isBlank()) {
      violations.add("app.storage.s3.bucket must be configured in prod");
    }
    if (properties.s3().region().isBlank()) {
      violations.add("app.storage.s3.region must be configured in prod");
    }
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "production storage readiness check failed:\n - " + String.join("\n - ", violations));
    }
  }
}
