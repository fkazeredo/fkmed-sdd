package com.fkmed.domain.upload;

/**
 * Provider-neutral storage port for uploaded bytes (SPEC-0019).
 *
 * <p>Returned references are opaque to business modules. Infrastructure qualifies them with the
 * owning provider so reads and deletes remain valid when the configured write backend changes.
 */
public interface FileStorage {

  /** Stores content under a server-controlled namespace and returns its opaque reference. */
  String store(StorageNamespace namespace, byte[] content);

  /** Reads all bytes identified by a previously returned reference. */
  byte[] read(String reference);

  /**
   * Deletes a reference. Inside a transaction, physical deletion is deferred until database commit
   * so rollback cannot leave metadata pointing at missing content.
   */
  void delete(String reference);
}
