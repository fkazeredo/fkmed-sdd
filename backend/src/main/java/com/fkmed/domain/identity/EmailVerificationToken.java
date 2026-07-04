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
 * A one-time e-mail verification link (SPEC-0002 BR5). Only the SHA-256 hash of the token is
 * stored; the raw token travels in the e-mail. Usable while unused and unexpired; opening it
 * activates the account, resending invalidates the previous one.
 */
@Entity
@Table(name = "email_verification_token")
@Getter
public class EmailVerificationToken {

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
  protected EmailVerificationToken() {}

  private EmailVerificationToken(
      UUID id, UUID accountId, String tokenHash, Instant issuedAt, Instant expiresAt) {
    this.id = id;
    this.accountId = accountId;
    this.tokenHash = tokenHash;
    this.createdAt = issuedAt;
    this.expiresAt = expiresAt;
  }

  /**
   * Issues a token for {@code accountId}, valid for {@code ttl} from {@code issuedAt} (BR5, 24h).
   */
  public static EmailVerificationToken issue(
      UUID accountId, String tokenHash, Instant issuedAt, Duration ttl) {
    return new EmailVerificationToken(
        UUID.randomUUID(), accountId, tokenHash, issuedAt, issuedAt.plus(ttl));
  }

  /** True when the link may still be opened: not used and not past its expiry. */
  public boolean isUsable(Instant now) {
    return usedAt == null && now.isBefore(expiresAt);
  }

  /** Marks the link consumed so it can never be reused (single-use, BR5). */
  public void markUsed(Instant when) {
    if (usedAt == null) {
      this.usedAt = when;
    }
  }
}
