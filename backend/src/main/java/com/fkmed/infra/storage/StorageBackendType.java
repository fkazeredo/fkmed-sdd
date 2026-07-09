package com.fkmed.infra.storage;

/** Selectable physical backend for new file writes. */
public enum StorageBackendType {
  POSTGRES("postgres"),
  FILESYSTEM("filesystem"),
  S3("s3");

  private final String referencePrefix;

  StorageBackendType(String referencePrefix) {
    this.referencePrefix = referencePrefix;
  }

  String referencePrefix() {
    return referencePrefix;
  }
}
