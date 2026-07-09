package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fkmed.domain.upload.FileStorage;
import com.fkmed.domain.upload.FileStorageException;
import com.fkmed.domain.upload.StorageNamespace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** SPEC-0019: retained PostgreSQL adapter and V28 backfill against a real database. */
class FileStorageAdapterIT extends AbstractIntegrationTest {

  @Autowired private FileStorage storage;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void postgresAdapterStoresReadsAndDeletesThroughThePort() {
    byte[] content = {1, 2, 3, 4};

    String reference = storage.store(StorageNamespace.PROFILE_PHOTO, content);

    assertThat(reference).startsWith("postgres:profile-photo/");
    assertThat(storage.read(reference)).isEqualTo(content);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from file_blob where object_key = ?",
                Integer.class,
                reference.substring("postgres:".length())))
        .isEqualTo(1);

    storage.delete(reference);
    assertThatThrownBy(() -> storage.read(reference)).isInstanceOf(FileStorageException.class);
  }

  @Test
  void migrationBackfillsSeededBytesAndRemovesBusinessBinaryColumns() {
    Integer backfilled =
        jdbc.queryForObject(
            "select count(*) from file_blob where object_key like 'reimbursement-document/%'",
            Integer.class);
    Integer legacyColumns =
        jdbc.queryForObject(
            """
            select count(*)
            from information_schema.columns
            where table_schema = 'public'
              and ((table_name = 'beneficiary_photo' and column_name = 'image')
                or (table_name in ('appointment_attachment', 'reimbursement_document',
                                   'preview_document') and column_name = 'content'))
            """,
            Integer.class);

    assertThat(backfilled).isPositive();
    assertThat(legacyColumns).isZero();
  }
}
