package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * A Meus Agendamentos card (SPEC-0009 BR13): type, specialty/exam, beneficiary, unit, date/time and
 * the effective status (derived {@code REALIZADO} once the start passed — BR12). The specialty is
 * carried as its registry code (the client already holds the specialty catalog to render the
 * label); the exam carries both code and label from this module's own registry.
 */
public record AppointmentView(
    UUID id,
    String protocol,
    AppointmentType type,
    String specialtyCode,
    String examCode,
    String examName,
    UUID beneficiaryId,
    String beneficiaryName,
    UUID unitId,
    String unitName,
    Instant scheduledAt,
    AppointmentStatus status,
    String cancelReason) {}
