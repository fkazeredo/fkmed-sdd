package com.fkmed.infra.storage;

import com.fkmed.domain.upload.FileStorage;
import com.fkmed.domain.upload.FileStorageException;
import com.fkmed.domain.upload.StorageNamespace;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Routes provider-qualified references while selecting one backend for new writes. */
@Component
@Slf4j
public final class RoutingFileStorage implements FileStorage {

  private static final Pattern KEY =
      Pattern.compile(
          "[a-z][a-z0-9-]{1,40}/" + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

  private final StorageBackendType writeBackend;
  private final Map<StorageBackendType, StorageBackendAdapter> adapters;
  private final MeterRegistry metrics;

  RoutingFileStorage(
      StorageProperties properties, List<StorageBackendAdapter> available, MeterRegistry metrics) {
    this.writeBackend = properties.backend();
    EnumMap<StorageBackendType, StorageBackendAdapter> indexed =
        new EnumMap<>(StorageBackendType.class);
    for (StorageBackendAdapter adapter : available) {
      if (indexed.put(adapter.type(), adapter) != null) {
        throw new IllegalStateException("duplicate storage adapter: " + adapter.type());
      }
    }
    this.adapters = Map.copyOf(indexed);
    this.metrics = metrics;
    requireAdapter(writeBackend);
  }

  @Override
  public String store(StorageNamespace namespace, byte[] content) {
    if (namespace == null || content == null || content.length == 0) {
      throw new FileStorageException("storage namespace and non-empty content are required");
    }
    StorageBackendAdapter adapter = requireAdapter(writeBackend);
    String key = namespace.path() + "/" + UUID.randomUUID();
    try {
      run(writeBackend, "write", () -> adapter.put(key, content));
    } catch (RuntimeException writeFailure) {
      try {
        run(writeBackend, "failed-write-delete", () -> adapter.delete(key));
      } catch (RuntimeException cleanupFailure) {
        writeFailure.addSuppressed(cleanupFailure);
      }
      throw writeFailure;
    }
    compensateOnRollback(adapter, key);
    return writeBackend.referencePrefix() + ":" + key;
  }

  @Override
  public byte[] read(String reference) {
    StorageLocation location = locationOf(reference);
    StorageBackendAdapter adapter = requireAdapter(location.backend());
    return call(location.backend(), "read", () -> adapter.get(location.key()));
  }

  @Override
  public void delete(String reference) {
    StorageLocation location = locationOf(reference);
    StorageBackendAdapter adapter = requireAdapter(location.backend());
    if (location.backend() == StorageBackendType.POSTGRES) {
      run(location.backend(), "delete", () -> adapter.delete(location.key()));
      return;
    }
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              try {
                run(location.backend(), "delete", () -> adapter.delete(location.key()));
              } catch (RuntimeException exception) {
                log.error(
                    "storage cleanup failed after commit for backend {}",
                    location.backend(),
                    exception);
              }
            }
          });
      return;
    }
    run(location.backend(), "delete", () -> adapter.delete(location.key()));
  }

  private void compensateOnRollback(StorageBackendAdapter adapter, String key) {
    if (writeBackend == StorageBackendType.POSTGRES
        || !TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              try {
                run(writeBackend, "rollback-delete", () -> adapter.delete(key));
              } catch (RuntimeException exception) {
                log.error(
                    "storage rollback cleanup failed for backend {}", writeBackend, exception);
              }
            }
          }
        });
  }

  private StorageBackendAdapter requireAdapter(StorageBackendType backend) {
    StorageBackendAdapter adapter = adapters.get(backend);
    if (adapter == null) {
      throw new FileStorageException("storage backend is not available: " + backend);
    }
    return adapter;
  }

  private static StorageLocation locationOf(String reference) {
    if (reference == null) {
      throw new FileStorageException("invalid storage reference");
    }
    int separator = reference.indexOf(':');
    if (separator <= 0 || separator == reference.length() - 1) {
      throw new FileStorageException("invalid storage reference");
    }
    String prefix = reference.substring(0, separator);
    StorageBackendType backend =
        Arrays.stream(StorageBackendType.values())
            .filter(type -> type.referencePrefix().equals(prefix))
            .findFirst()
            .orElseThrow(() -> new FileStorageException("unknown storage reference backend"));
    String key = reference.substring(separator + 1);
    if (!KEY.matcher(key).matches()) {
      throw new FileStorageException("invalid storage reference key");
    }
    return new StorageLocation(backend, key);
  }

  private void run(StorageBackendType backend, String operation, Runnable action) {
    call(
        backend,
        operation,
        () -> {
          action.run();
          return null;
        });
  }

  private <T> T call(StorageBackendType backend, String operation, Operation<T> action) {
    try {
      T result = action.execute();
      increment(backend, operation, "success");
      return result;
    } catch (RuntimeException exception) {
      increment(backend, operation, "failure");
      throw exception;
    }
  }

  private void increment(StorageBackendType backend, String operation, String outcome) {
    metrics
        .counter(
            "storage.operation",
            "backend",
            backend.referencePrefix(),
            "operation",
            operation,
            "outcome",
            outcome)
        .increment();
  }

  private record StorageLocation(StorageBackendType backend, String key) {}

  @FunctionalInterface
  private interface Operation<T> {
    T execute();
  }
}
