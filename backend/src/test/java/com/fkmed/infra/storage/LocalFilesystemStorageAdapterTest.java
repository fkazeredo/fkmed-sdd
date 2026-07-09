package com.fkmed.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fkmed.domain.upload.FileStorageException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilesystemStorageAdapterTest {

  @TempDir Path root;

  @Test
  void storesReadsReplacesAndDeletesBelowConfiguredRoot() {
    LocalFilesystemStorageAdapter adapter = adapter();
    String key = "profile-photo/12345678-1234-4234-8234-123456789abc";

    adapter.put(key, new byte[] {1, 2});
    assertThat(adapter.get(key)).containsExactly(1, 2);
    assertThat(root.resolve(key)).exists();

    adapter.put(key, new byte[] {3, 4});
    assertThat(adapter.get(key)).containsExactly(3, 4);

    adapter.delete(key);
    assertThat(root.resolve(key)).doesNotExist();
  }

  @Test
  void rejectsTraversalOutsideConfiguredRoot() {
    LocalFilesystemStorageAdapter adapter = adapter();

    assertThatThrownBy(() -> adapter.get("../../secret"))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("escapes");
  }

  private LocalFilesystemStorageAdapter adapter() {
    return new LocalFilesystemStorageAdapter(
        new StorageProperties(
            StorageBackendType.FILESYSTEM,
            new StorageProperties.Filesystem(root.toString()),
            new StorageProperties.S3Settings("", "", "", "", "", false)));
  }
}
