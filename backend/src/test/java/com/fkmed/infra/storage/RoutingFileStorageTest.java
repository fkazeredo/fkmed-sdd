package com.fkmed.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fkmed.domain.upload.FileStorageException;
import com.fkmed.domain.upload.StorageNamespace;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RoutingFileStorageTest {

  private static final String UUID = "12345678-1234-4234-8234-123456789abc";
  private static final byte[] CONTENT = {1, 2, 3};

  @AfterEach
  void clearSynchronization() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void configuredBackendOwnsWrites_butReferencePrefixOwnsReads() {
    FakeAdapter postgres = new FakeAdapter(StorageBackendType.POSTGRES);
    FakeAdapter filesystem = new FakeAdapter(StorageBackendType.FILESYSTEM);
    postgres.content.put("profile-photo/" + UUID, CONTENT);
    RoutingFileStorage storage = storage(StorageBackendType.FILESYSTEM, postgres, filesystem);

    String written = storage.store(StorageNamespace.PROFILE_PHOTO, CONTENT);

    assertThat(written).startsWith("filesystem:profile-photo/");
    assertThat(filesystem.content).hasSize(1);
    assertThat(storage.read("postgres:profile-photo/" + UUID)).isEqualTo(CONTENT);
  }

  @Test
  void newlyWrittenObjectIsCompensatedAfterRollback() {
    FakeAdapter filesystem = new FakeAdapter(StorageBackendType.FILESYSTEM);
    RoutingFileStorage storage = storage(StorageBackendType.FILESYSTEM, filesystem);
    TransactionSynchronizationManager.initSynchronization();

    String reference = storage.store(StorageNamespace.APPOINTMENT_ORDER, CONTENT);
    complete(TransactionSynchronization.STATUS_ROLLED_BACK);

    assertThat(filesystem.content).isEmpty();
    assertThat(reference).startsWith("filesystem:appointment-order/");
  }

  @Test
  void ambiguousWriteFailureGetsImmediateBestEffortCleanup() {
    FakeAdapter filesystem = new FakeAdapter(StorageBackendType.FILESYSTEM);
    filesystem.failAfterPut = true;
    RoutingFileStorage storage = storage(StorageBackendType.FILESYSTEM, filesystem);

    assertThatThrownBy(() -> storage.store(StorageNamespace.PROFILE_PHOTO, CONTENT))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("ambiguous write");
    assertThat(filesystem.content).isEmpty();
  }

  @Test
  void existingObjectIsDeletedOnlyAfterCommit() {
    FakeAdapter filesystem = new FakeAdapter(StorageBackendType.FILESYSTEM);
    filesystem.content.put("profile-photo/" + UUID, CONTENT);
    RoutingFileStorage storage = storage(StorageBackendType.FILESYSTEM, filesystem);
    TransactionSynchronizationManager.initSynchronization();

    storage.delete("filesystem:profile-photo/" + UUID);
    assertThat(filesystem.content).containsKey("profile-photo/" + UUID);

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);
    assertThat(filesystem.content).isEmpty();
  }

  @Test
  void postgresDeleteParticipatesInTheCurrentDatabaseTransactionImmediately() {
    FakeAdapter postgres = new FakeAdapter(StorageBackendType.POSTGRES);
    postgres.content.put("profile-photo/" + UUID, CONTENT);
    RoutingFileStorage storage = storage(StorageBackendType.POSTGRES, postgres);
    TransactionSynchronizationManager.initSynchronization();

    storage.delete("postgres:profile-photo/" + UUID);

    assertThat(postgres.content).isEmpty();
    assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
  }

  @Test
  void malformedOrUnknownReferenceIsRejectedBeforeAnyAdapterCall() {
    RoutingFileStorage storage =
        storage(StorageBackendType.POSTGRES, new FakeAdapter(StorageBackendType.POSTGRES));

    assertThatThrownBy(() -> storage.read("s3:../../secret"))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("invalid storage reference key");
    assertThatThrownBy(() -> storage.read("other:profile-photo/" + UUID))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("unknown storage reference backend");
  }

  private static RoutingFileStorage storage(
      StorageBackendType writeBackend, FakeAdapter... adapters) {
    StorageProperties properties =
        new StorageProperties(
            writeBackend,
            new StorageProperties.Filesystem("/fkmed/uploads"),
            new StorageProperties.S3Settings("", "", "fkmed", "", "", false));
    return new RoutingFileStorage(properties, List.of(adapters), new SimpleMeterRegistry());
  }

  private static void complete(int status) {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(synchronization -> synchronization.afterCompletion(status));
  }

  private static final class FakeAdapter implements StorageBackendAdapter {

    private final StorageBackendType type;
    private final Map<String, byte[]> content = new java.util.HashMap<>();
    private boolean failAfterPut;

    private FakeAdapter(StorageBackendType type) {
      this.type = type;
    }

    @Override
    public StorageBackendType type() {
      return type;
    }

    @Override
    public void put(String key, byte[] bytes) {
      content.put(key, bytes);
      if (failAfterPut) {
        throw new FileStorageException("ambiguous write failure");
      }
    }

    @Override
    public byte[] get(String key) {
      return content.get(key);
    }

    @Override
    public void delete(String key) {
      content.remove(key);
    }
  }
}
