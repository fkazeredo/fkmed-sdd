package com.fkmed.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A one-time password-reset link (SPEC-0002 BR10). Only the SHA-256 hash of the token is stored;
 * the raw token travels in the reset e-mail. Usable while unused and unexpired (30-min TTL);
 * consuming it resets the password, requesting a new one invalidates the previous one. Mirrors
 * {@link EmailVerificationToken}.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
public class PasswordResetToken {

  @Id private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** JPA only. */
  protected PasswordResetToken() {}

  private PasswordResetToken(
      UUID id, UUID accountId, String tokenHash, Instant issuedAt, Instant expiresAt) {
    this.id = id;
    this.accountId = accountId;
    this.tokenHash = tokenHash;
    this.createdAt = issuedAt;
    this.expiresAt = expiresAt;
  }

  /** Issues a token for {@code accountId}, valid for {@code ttl} from {@code issuedAt} (BR10). */
  public static PasswordResetToken issue(
      UUID accountId, String tokenHash, Instant issuedAt, Duration ttl) {
    return new PasswordResetToken(
        UUID.randomUUID(), accountId, tokenHash, issuedAt, issuedAt.plus(ttl));
  }

  /** True when the link may still be used: not used and not past its expiry. */
  public boolean isUsable(Instant now) {
    return usedAt == null && now.isBefore(expiresAt);
  }

  /** Marks the link consumed so it can never be reused (single-use, BR10). */
  public void markUsed(Instant when) {
    if (usedAt == null) {
      this.usedAt = when;
    }
  }
}
