package com.fkmed.domain.plan;

/**
 * Outcome of a contact update: the e-mail before and after, so the application service can decide
 * whether to publish {@link ContactDataChanged} (only when the contact e-mail actually changed).
 *
 * @param oldEmail the contact e-mail before the update (may be {@code null}).
 * @param newEmail the contact e-mail after the update (never {@code null} — BR6).
 */
public record ContactChange(String oldEmail, String newEmail) {

  /** Whether the contact e-mail actually changed (case-insensitive on the local convention). */
  public boolean emailChanged() {
    return !newEmail.equals(oldEmail);
  }
}
