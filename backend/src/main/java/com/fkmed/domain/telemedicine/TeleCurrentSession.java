package com.fkmed.domain.telemedicine;

import java.util.UUID;

/**
 * The caller's current session as the delivery layer needs it: the {@code sessionId} to target SSE
 * re-emits (ADR-0016/DL-0022) plus the {@link TeleSessionView} payload the beneficiary sees.
 */
public record TeleCurrentSession(UUID sessionId, TeleSessionView view) {}
