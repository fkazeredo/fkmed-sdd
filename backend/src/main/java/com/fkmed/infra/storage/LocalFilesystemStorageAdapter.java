package com.fkmed.infra.storage;

import com.fkmed.domain.upload.FileStorageException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;

/** Local private filesystem adapter rooted below the configured FKMed directory. */
@Component
final class LocalFilesystemStorageAdapter implements StorageBackendAdapter {

  private final Path root;

  LocalFilesystemStorageAdapter(StorageProperties properties) {
    this.root = Path.of(properties.filesystem().root()).toAbsolutePath().normalize();
  }

  @Override
  public StorageBackendType type() {
    return StorageBackendType.FILESYSTEM;
  }

  @Override
  public void put(String key, byte[] content) {
    Path target = resolve(key);
    Path temporary = null;
    try {
      Files.createDirectories(target.getParent());
      temporary = Files.createTempFile(target.getParent(), ".fkmed-upload-", ".tmp");
      Files.write(temporary, content);
      moveAtomically(temporary, target);
    } catch (IOException exception) {
      throw new FileStorageException("could not store filesystem content", exception);
    } finally {
      deleteTemporary(temporary);
    }
  }

  @Override
  public byte[] get(String key) {
    try {
      return Files.readAllBytes(resolve(key));
    } catch (IOException exception) {
      throw new FileStorageException("could not read filesystem content", exception);
    }
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException exception) {
      throw new FileStorageException("could not delete filesystem content", exception);
    }
  }

  private Path resolve(String key) {
    Path resolved = root.resolve(key).normalize();
    if (!resolved.startsWith(root)) {
      throw new FileStorageException("storage key escapes the configured filesystem root");
    }
    return resolved;
  }

  private static void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void deleteTemporary(Path temporary) {
    if (temporary == null) {
      return;
    }
    try {
      Files.deleteIfExists(temporary);
    } catch (IOException ignored) {
      // Best effort after a failed write; the primary exception remains authoritative.
    }
  }
}
