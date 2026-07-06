package com.fkmed.application.sim;

import java.util.List;
import java.util.UUID;

/**
 * The result of an operator tele action (SPEC-0018 BR5): the affected session id and its new state,
 * plus the ids of any documents issued at closure (empty for start-attending).
 */
public record SimTeleSessionResult(UUID sessionId, String state, List<UUID> issuedDocumentIds) {}
