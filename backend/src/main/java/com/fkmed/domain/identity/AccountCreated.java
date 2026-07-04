package com.fkmed.domain.identity;

import java.util.UUID;

/**
 * Domain event: a first-access account was created and awaits e-mail verification (SPEC-0002
 * §Events). Published AFTER_COMMIT and consumed by the infra e-mail listener (ADR-0004; SPEC-0004
 * will centralize delivery). The payload carries only the account id, delivery e-mail and the raw
 * one-time verification token — never the password or full CPF.
 *
 * @param accountId the created account.
 * @param email the delivery e-mail (the account's login e-mail).
 * @param beneficiaryId the beneficiary the account belongs to.
 * @param verificationToken the raw verification token to embed in the link.
 */
public record AccountCreated(
    UUID accountId, String email, UUID beneficiaryId, String verificationToken) {}
