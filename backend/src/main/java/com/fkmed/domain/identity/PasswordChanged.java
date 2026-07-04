package com.fkmed.domain.identity;

import java.util.UUID;

/**
 * Domain event: an account's password changed (SPEC-0002 §Events, BR10/BR11). Published AFTER_COMMIT
 * and consumed by the infra e-mail listener to deliver the "se não foi você, contate os canais"
 * security notice (ADR-0004; SPEC-0004 will centralize delivery). Serves both the recovery reset and
 * the authenticated self-change, distinguished by {@code flow} (DL-0003). The payload carries only
 * the account id, delivery e-mail and beneficiary — never the password.
 *
 * @param accountId the account whose password changed.
 * @param email the delivery e-mail (the account's login e-mail).
 * @param beneficiaryId the beneficiary the account belongs to.
 * @param flow how the change happened — {@link #RECOVERY_RESET} or {@link #SELF_CHANGE}.
 */
public record PasswordChanged(UUID accountId, String email, UUID beneficiaryId, String flow) {

  /** The change came from the password-recovery reset flow (BR10). */
  public static final String RECOVERY_RESET = "recovery-reset";

  /** The change came from the authenticated self-service change (BR11). */
  public static final String SELF_CHANGE = "self-change";
}
