package com.fkmed.domain.identity;

import java.util.UUID;

/**
 * Domain event: a password recovery was legitimately requested for an existing active account
 * (SPEC-0002 §Events, BR10). Published AFTER_COMMIT and consumed by the infra e-mail listener to
 * deliver the 30-minute reset link (ADR-0004; SPEC-0004 will centralize delivery). Emitted only when
 * the e-mail matched an account — never for a non-existent e-mail — while the HTTP response stays
 * neutral either way (BR7). The payload carries the account id, delivery e-mail and the raw one-time
 * reset token — never the password.
 *
 * @param accountId the account the reset applies to.
 * @param email the delivery e-mail (the account's login e-mail).
 * @param beneficiaryId the beneficiary the account belongs to.
 * @param resetToken the raw one-time reset token to embed in the link.
 */
public record PasswordRecoveryRequested(
    UUID accountId, String email, UUID beneficiaryId, String resetToken) {}
