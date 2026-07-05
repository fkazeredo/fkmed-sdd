package com.fkmed.integration;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Disposable login account for PEDRO — a dependent with no Flyway-seeded account — bound to his
 * canonical beneficiary, so the server-side card resolution of ADR-0009 yields PEDRO's card when a
 * test authenticates as {@link #PEDRO_EMAIL}. {@link #remove} also clears any account a sibling IT
 * leaked on PEDRO's beneficiary (with its child rows), freeing the unique {@code beneficiary_id}
 * slot and satisfying the account foreign keys.
 */
final class PedroAccountFixture {

  static final String PEDRO_EMAIL = "pedro@fkmed.local";
  private static final String PEDRO_ACCOUNT_ID = "c1a2b3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d";
  private static final String PEDRO_BENEFICIARY_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  private PedroAccountFixture() {}

  /** Creates PEDRO's active account bound to his beneficiary; idempotent (removes first). */
  static void seed(JdbcTemplate jdbc) {
    remove(jdbc);
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status,"
            + " failed_attempts, created_at) values (?::uuid, ?::uuid, ?,"
            + " '{bcrypt}' || crypt(gen_random_uuid()::text, gen_salt('bf', 4)), 'ACTIVE', 0, now())",
        PEDRO_ACCOUNT_ID,
        PEDRO_BENEFICIARY_ID,
        PEDRO_EMAIL);
  }

  /** Removes any account on PEDRO's beneficiary together with its child rows (FK-safe). */
  static void remove(JdbcTemplate jdbc) {
    String childScope =
        " where account_id in (select id from user_account where beneficiary_id = ?::uuid)";
    jdbc.update("delete from email_verification_token" + childScope, PEDRO_BENEFICIARY_ID);
    jdbc.update("delete from password_reset_token" + childScope, PEDRO_BENEFICIARY_ID);
    jdbc.update("delete from term_acceptance" + childScope, PEDRO_BENEFICIARY_ID);
    jdbc.update("delete from user_account where beneficiary_id = ?::uuid", PEDRO_BENEFICIARY_ID);
  }
}
