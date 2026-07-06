package com.fkmed.domain.telemedicine;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: the session was closed by the professional (SPEC-0010 §Events, BR9/BR10). Published
 * inside the {@link TeleService#close} transaction. Wave 2 wires two AFTER_COMMIT consumers off it:
 * the notification "session closed" notice and the clinical-document issuance into {@code
 * domain.clinicaldocs} (bound to the attended beneficiary and this session). This module only
 * publishes it.
 *
 * @param guidance the professional's closing guidance, may be {@code null}.
 * @param authorAccountId the account that opened the session (SPEC-0003 author), may be {@code
 *     null}.
 */
public record TeleSessionClosed(
    UUID sessionId,
    UUID beneficiaryId,
    String professionalName,
    String professionalCrm,
    String guidance,
    UUID authorAccountId,
    Instant endedAt) {}
