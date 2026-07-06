package com.fkmed.domain.appointment;

import java.util.List;

/**
 * Meus Agendamentos (SPEC-0009 BR13): the two tabs — {@code upcoming} (active commitments, soonest
 * first) and {@code history} (cancelled and realised, most recent first) — across all the
 * beneficiaries the caller may act for, already split and ordered server-side.
 */
public record AppointmentListResponse(
    List<AppointmentView> upcoming, List<AppointmentView> history) {}
