package com.fkmed.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0002 BR5: the account lifecycle EMAIL_NOT_VERIFIED → ACTIVE. */
class UserAccountTest {

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
}
