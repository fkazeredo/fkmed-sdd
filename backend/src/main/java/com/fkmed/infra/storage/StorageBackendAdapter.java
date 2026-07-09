package com.fkmed.infra.storage;

interface StorageBackendAdapter {

  StorageBackendType type();

  void put(String key, byte[] content);

  byte[] get(String key);

  void delete(String key);
}
