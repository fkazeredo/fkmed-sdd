package com.fkmed.domain.audit;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A single audit record to append (SPEC-0003 BR6). Immutable value passed to {@link AuditRecorder};
 * {@code details} MUST already be masked (SPEC-0003 BR8 — use {@link Masking}).
 *
 * @param eventType the {@link AuditEventTypes} code of the recorded fact (required).
 * @param authorAccountId the acting user account, or {@code null} when unknown (e.g. a failed login
 *     for a non-existent e-mail).
 * @param targetBeneficiaryId the beneficiary the action concerns, or {@code null}.
 * @param details additional masked, non-sensitive key/values (defaults to empty).
 * @param context request-origin metadata (IP, user agent); defaults to {@link AuditContext#none()}.
 */
public record AuditEntry(
    String eventType,
    UUID authorAccountId,
    UUID targetBeneficiaryId,
    Map<String, String> details,
    AuditContext context) {

  public AuditEntry {
    Objects.requireNonNull(eventType, "eventType is required");
    details = details == null ? Map.of() : Map.copyOf(details);
    context = context == null ? AuditContext.none() : context;
  }
}
