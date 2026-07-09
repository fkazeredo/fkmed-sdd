package com.fkmed.domain.upload;

/** Infrastructure storage failure translated to the generic API error contract. */
public final class FileStorageException extends RuntimeException {

  public FileStorageException(String message) {
    super(message);
  }

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
