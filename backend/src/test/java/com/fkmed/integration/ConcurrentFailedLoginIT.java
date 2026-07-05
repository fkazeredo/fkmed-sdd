package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.identity.IdentityService;
import com.fkmed.domain.identity.UserAccount;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Débito técnico A (optimistic lock on {@code user_account}) — the concurrency regression that
 * proves the lost-update fix (SPEC-0002 BR8 under contention). N simultaneous wrong-password
 * attempts on the SAME disposable account must still drive the counter to the lock threshold and
 * leave the account locked.
 *
 * <p>RED before the fix: without {@code @Version} the concurrent {@code recordFailedLogin}
 * transactions read a stale {@code failed_attempts} and overwrite each other, so the counter ends
 * below {@link UserAccount#MAX_FAILED_ATTEMPTS} and the account never locks. GREEN after the fix:
 * {@code @Version} turns the collision into an {@code ObjectOptimisticLockingFailureException} that
 * the bounded retry re-applies on a fresh read, so every increment is preserved and the 5th failure
 * locks the account (DL-0005).
 *
 * <p>Isolation: the shared Postgres tables are cleaned in {@code @BeforeEach} (not only after) — a
 * sibling class must never leave a locked/incremented row behind for this absolute-count assertion.
 */
class ConcurrentFailedLoginIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String ACCOUNT_EMAIL = "concurrency-it@fkmed.local";
  private static final String PEDRO_BENEFICIARY_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
  private static final int CONCURRENT_ATTEMPTS = 8;

  @Autowired private IdentityService identityService;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void isolate() {
    jdbc.update("delete from audit_event");
    jdbc.update("delete from password_reset_token");
    jdbc.update("delete from email_verification_token");
    jdbc.update("delete from term_acceptance");
    jdbc.update("delete from user_account where email <> ?", MARIA_EMAIL);
    seedActiveAccount();
  }

  @Test
  void concurrentWrongPasswords_stillReachTheLockThreshold_withoutLostIncrements()
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
    CountDownLatch ready = new CountDownLatch(CONCURRENT_ATTEMPTS);
    CountDownLatch fire = new CountDownLatch(1);
    AtomicInteger failures = new AtomicInteger();
    try {
      for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
        pool.submit(
            () -> {
              ready.countDown();
              try {
                fire.await();
                identityService.recordFailedLogin(ACCOUNT_EMAIL, AuditContext.none());
              } catch (RuntimeException | InterruptedException e) {
                failures.incrementAndGet();
              }
            });
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      fire.countDown();
    } finally {
      pool.shutdown();
      assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(failures.get()).as("no attempt should surface a raw framework exception").isZero();
    assertThat(failedAttempts(ACCOUNT_EMAIL))
        .as("every concurrent increment must be preserved up to the lock threshold")
        .isEqualTo(UserAccount.MAX_FAILED_ATTEMPTS);
    assertThat(lockedUntil(ACCOUNT_EMAIL)).as("the account must end locked").isNotNull();
    assertThat(auditCount("identity.account-locked"))
        .as("the lock is audited exactly once")
        .isEqualTo(1);
  }

  private void seedActiveAccount() {
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status,"
            + " failed_attempts, created_at) values (gen_random_uuid(), ?::uuid, ?,"
            + " '{bcrypt}' || crypt('Senha1234', gen_salt('bf', 10)), 'ACTIVE', 0, now())",
        PEDRO_BENEFICIARY_ID,
        ACCOUNT_EMAIL);
  }

  private int failedAttempts(String email) {
    return jdbc.queryForObject(
        "select failed_attempts from user_account where email = ?", Integer.class, email);
  }

  private Instant lockedUntil(String email) {
    return jdbc.queryForObject(
        "select locked_until from user_account where email = ?", Instant.class, email);
  }

  private long auditCount(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ?", Long.class, eventType);
  }
}
