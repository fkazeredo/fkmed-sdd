package com.fkmed.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary's personal access account (SPEC-0002). Created in {@link
 * AccountStatus#EMAIL_NOT_VERIFIED} and mutated only through the meaningful {@link #activate()}
 * transition — never a setter (baseline §0013). The {@code beneficiaryId} is a cross-context id
 * value (no JPA relationship crosses the module — ADR-0001). Lockout columns exist for SLICE 1.2.
 */
@Entity
@Table(name = "user_account")
@Getter
public class UserAccount {

  @Id private UUID id;

  @Column(name = "beneficiary_id", nullable = false, unique = true, updatable = false)
  private UUID beneficiaryId;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountStatus status;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** JPA only. */
  protected UserAccount() {}

  private UserAccount(
      UUID id, UUID beneficiaryId, String email, String passwordHash, Instant createdAt) {
    this.id = id;
    this.beneficiaryId = beneficiaryId;
    this.email = email;
    this.passwordHash = passwordHash;
    this.status = AccountStatus.EMAIL_NOT_VERIFIED;
    this.failedAttempts = 0;
    this.createdAt = createdAt;
  }

  /** Registers a new, not-yet-verified account for a beneficiary (BR5). */
  public static UserAccount register(
      UUID beneficiaryId, String email, String passwordHash, Instant createdAt) {
    return new UserAccount(UUID.randomUUID(), beneficiaryId, email, passwordHash, createdAt);
  }

  /** Activates the account once its e-mail is verified (BR5). Idempotent. */
  public void activate() {
    this.status = AccountStatus.ACTIVE;
  }

  public boolean isActive() {
    return status == AccountStatus.ACTIVE;
  }
}
