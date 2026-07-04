package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0002 BR5 account lifecycle (EMAIL_NOT_VERIFIED → ACTIVE) and BR8 lockout state machine
 * (counter/window semantics, DL-0002).
 */
class UserAccountTest {

  private static final Instant T0 = Instant.parse("2026-07-04T12:00:00Z");
  private static final Duration LOCK = Duration.ofMinutes(15);

  private static UserAccount account() {
    return UserAccount.register(UUID.randomUUID(), "user@fkmed.local", "{bcrypt}hash", T0);
  }

  @Test
  void register_startsUnverified() {
    UserAccount account =
        UserAccount.register(UUID.randomUUID(), "user@fkmed.local", "{bcrypt}hash", Instant.now());
    assertThat(account.getStatus()).isEqualTo(AccountStatus.EMAIL_NOT_VERIFIED);
    assertThat(account.isActive()).isFalse();
    assertThat(account.getFailedAttempts()).isZero();
  }

  @Test
  void activate_movesToActive() {
    UserAccount account =
        UserAccount.register(UUID.randomUUID(), "user@fkmed.local", "{bcrypt}hash", Instant.now());
    account.activate();
    assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(account.isActive()).isTrue();
  }

  @Test
  void fourFailures_doNotLock() {
    UserAccount account = account();
    for (int i = 0; i < 4; i++) {
      account.registerFailedLogin(T0);
    }
    assertThat(account.getFailedAttempts()).isEqualTo(4);
    assertThat(account.isLocked(T0)).isFalse();
    assertThat(account.getLockedUntil()).isNull();
  }

  @Test
  void theFifthConsecutiveFailure_locksForFifteenMinutes() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }
    assertThat(account.getFailedAttempts()).isEqualTo(5);
    assertThat(account.isLocked(T0)).isTrue();
    assertThat(account.getLockedUntil()).isEqualTo(T0.plus(LOCK));
  }

  @Test
  void isLocked_isTrueJustBeforeTheWindow_andFalseExactlyAtTheBoundary() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }
    Instant boundary = T0.plus(LOCK);
    assertThat(account.isLocked(boundary.minusMillis(1))).isTrue();
    assertThat(account.isLocked(boundary)).isFalse();
  }

  @Test
  void attemptsWhileLocked_doNotExtendTheWindowNorIncrementTheCounter() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }
    Instant lockedUntil = account.getLockedUntil();

    // Hammering inside the window (DL-0002 reading 1): no-op, so the window stays anchored to the
    // 5th failure and cannot be pushed out by an attacker or by noise.
    account.registerFailedLogin(T0.plus(Duration.ofMinutes(5)));
    account.registerFailedLogin(T0.plus(Duration.ofMinutes(10)));

    assertThat(account.getFailedAttempts()).isEqualTo(5);
    assertThat(account.getLockedUntil()).isEqualTo(lockedUntil);
  }

  @Test
  void afterExpiry_theCounterStaysAtFive_soTheNextFailureReLocksImmediately() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }
    Instant afterWindow = T0.plus(Duration.ofMinutes(16));
    assertThat(account.isLocked(afterWindow)).isFalse();

    // DL-0002 reading 2: expiry lifts the lock but only a SUCCESSFUL login clears the counter, so a
    // still-failing credential re-locks at once instead of getting a fresh five-try budget.
    account.registerFailedLogin(afterWindow);
    assertThat(account.isLocked(afterWindow)).isTrue();
    assertThat(account.getLockedUntil()).isEqualTo(afterWindow.plus(LOCK));
  }

  @Test
  void aSuccessfulLogin_resetsTheCounterAndClearsTheLock() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }

    account.registerSuccessfulLogin();

    assertThat(account.getFailedAttempts()).isZero();
    assertThat(account.getLockedUntil()).isNull();
    assertThat(account.isLocked(T0)).isFalse();
  }

  @Test
  void changePassword_setsTheHashAndClearsAnyLockState() {
    UserAccount account = account();
    for (int i = 0; i < 5; i++) {
      account.registerFailedLogin(T0);
    }

    account.changePassword("{bcrypt}new-hash");

    assertThat(account.getPasswordHash()).isEqualTo("{bcrypt}new-hash");
    assertThat(account.getFailedAttempts()).isZero();
    assertThat(account.getLockedUntil()).isNull();
  }
}
