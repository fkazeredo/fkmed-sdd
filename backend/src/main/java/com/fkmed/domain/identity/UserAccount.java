package com.fkmed.domain.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * A beneficiary's personal access account (SPEC-0002). Created in {@link
 * AccountStatus#EMAIL_NOT_VERIFIED} and mutated only through meaningful business transitions —
 * {@link #activate()}, {@link #registerFailedLogin(Instant)}, {@link #registerSuccessfulLogin()},
 * {@link #changePassword(String)} — never a setter (baseline §0013). The {@code beneficiaryId} is a
 * cross-context id value (no JPA relationship crosses the module — ADR-0001). Lockout (BR8) is a
 * small state machine over {@code failedAttempts}/{@code lockedUntil}.
 */
@Entity
@Table(name = "user_account")
@Getter
public class UserAccount {

  /** Consecutive failures that trigger a lock (SPEC-0002 BR8). */
  public static final int MAX_FAILED_ATTEMPTS = 5;

  /** How long the account stays locked once {@link #MAX_FAILED_ATTEMPTS} is reached (BR8). */
  public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

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

  /**
   * Optimistic-lock version (débito técnico A, DL-0005): concurrent lockout increments were
   * lost-updating each other; JPA now guards every mutation so a stale write fails and is retried
   * on a fresh read (Flyway V6, {@code default 0}). Read-only — no setter, bumped by the provider.
   */
  @Version private long version;

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

  /** True while a lock is in force at {@code now} (SPEC-0002 BR8). */
  public boolean isLocked(Instant now) {
    return lockedUntil != null && now.isBefore(lockedUntil);
  }

  /**
   * Records one failed login (SPEC-0002 BR8). Attempts made while the account is already locked are
   * ignored, so the 15-minute window is measured from the 5th failure and cannot be extended by
   * further attempts (DL-0002). The 5th consecutive failure locks the account for {@link
   * #LOCK_DURATION}.
   */
  public void registerFailedLogin(Instant now) {
    if (isLocked(now)) {
      return;
    }
    this.failedAttempts++;
    if (this.failedAttempts >= MAX_FAILED_ATTEMPTS) {
      this.lockedUntil = now.plus(LOCK_DURATION);
    }
  }

  /** Clears the failure counter and any lock after a successful login (SPEC-0002 BR8). */
  public void registerSuccessfulLogin() {
    this.failedAttempts = 0;
    this.lockedUntil = null;
  }

  /**
   * Sets a new password hash (SPEC-0002 BR10 reset / BR11 change) and clears any lock/failure state
   * so the user can authenticate immediately with the new credential.
   */
  public void changePassword(String newPasswordHash) {
    this.passwordHash = newPasswordHash;
    this.failedAttempts = 0;
    this.lockedUntil = null;
  }
}
