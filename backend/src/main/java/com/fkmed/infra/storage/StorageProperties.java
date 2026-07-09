package com.fkmed.infra.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Environment-bound file storage configuration (SPEC-0019). */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(StorageBackendType backend, Filesystem filesystem, S3Settings s3) {

  public StorageProperties {
    backend = backend == null ? StorageBackendType.POSTGRES : backend;
    filesystem = filesystem == null ? new Filesystem("/fkmed/uploads") : filesystem;
    s3 = s3 == null ? new S3Settings("", "", "fkmed", "", "", false) : s3;
  }

  public record Filesystem(String root) {
    public Filesystem {
      root = root == null || root.isBlank() ? "/fkmed/uploads" : root.strip();
    }
  }

  public record S3Settings(
      String bucket,
      String region,
      String prefix,
      String kmsKeyId,
      String endpoint,
      boolean pathStyle) {
    public S3Settings {
      bucket = clean(bucket);
      region = clean(region);
      prefix = clean(prefix);
      kmsKeyId = clean(kmsKeyId);
      endpoint = clean(endpoint);
    }

    private static String clean(String value) {
      return value == null ? "" : value.strip();
    }
  }
}
