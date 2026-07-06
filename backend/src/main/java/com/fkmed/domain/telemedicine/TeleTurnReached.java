package com.fkmed.domain.telemedicine;

import java.util.UUID;

/**
 * Domain event: it is the beneficiary's turn — the professional started attending and the room is
 * open (SPEC-0010 §Events, BR8). Published inside the {@link TeleService#reachTurn} transaction;
 * the notification module's listener (wired in Wave 2, AFTER_COMMIT) delivers the "your turn"
 * notice. This module only publishes it.
 *
 * @param authorAccountId the account that opened the session (SPEC-0003 author), may be {@code
 *     null}.
 */
public record TeleTurnReached(
    UUID sessionId, UUID beneficiaryId, String professionalName, UUID authorAccountId) {}
