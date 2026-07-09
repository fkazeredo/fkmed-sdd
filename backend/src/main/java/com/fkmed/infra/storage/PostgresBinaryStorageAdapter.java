package com.fkmed.infra.storage;

import com.fkmed.domain.upload.FileStorageException;
import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Retains the original PostgreSQL binary behavior behind the shared storage contract. */
@Component
final class PostgresBinaryStorageAdapter implements StorageBackendAdapter {

  private final JdbcTemplate jdbc;
  private final Clock clock;

  PostgresBinaryStorageAdapter(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public StorageBackendType type() {
    return StorageBackendType.POSTGRES;
  }

  @Override
  public void put(String key, byte[] content) {
    try {
      jdbc.update(
          "insert into file_blob (object_key, content, created_at) values (?, ?, ?)",
          key,
          content,
          clock.instant().atOffset(ZoneOffset.UTC));
    } catch (DataAccessException exception) {
      throw new FileStorageException("could not store PostgreSQL file content", exception);
    }
  }

  @Override
  public byte[] get(String key) {
    try {
      return jdbc.queryForObject(
          "select content from file_blob where object_key = ?", byte[].class, key);
    } catch (EmptyResultDataAccessException exception) {
      throw new FileStorageException("stored file content was not found", exception);
    } catch (DataAccessException exception) {
      throw new FileStorageException("could not read PostgreSQL file content", exception);
    }
  }

  @Override
  public void delete(String key) {
    try {
      jdbc.update("delete from file_blob where object_key = ?", key);
    } catch (DataAccessException exception) {
      throw new FileStorageException("could not delete PostgreSQL file content", exception);
    }
  }
}
