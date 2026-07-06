package com.fkmed.domain.telemedicine;

/**
 * The outcome of entering the Pronto Atendimento queue (SPEC-0010 POST /api/tele/sessions): the
 * session state with its queue position and estimated wait, plus whether an existing session was
 * resumed (BR7) — the delivery layer answers {@code 200} on a resume and {@code 201} on a new
 * entry.
 */
public record EnterQueueResult(String state, int position, int etaMinutes, boolean resumed) {}
