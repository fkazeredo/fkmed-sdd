package com.fkmed.domain.plan;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: a beneficiary's contact e-mail changed (SPEC-0006 §Events). Published AFTER_COMMIT
 * from the contact update so the SPEC-0004 notification listener (wired at integration) can send
 * the security notice to both the old and the new address. Owned by the plan module because the
 * change happens here; this module only publishes it and never handles notifications.
 *
 * @param beneficiaryId the beneficiary whose contact e-mail changed.
 * @param oldEmail the previous contact e-mail (may be {@code null} if none was set before).
 * @param newEmail the new contact e-mail.
 * @param changedAt when the change was committed.
 */
public record ContactDataChanged(
    UUID beneficiaryId, String oldEmail, String newEmail, Instant changedAt) {}
