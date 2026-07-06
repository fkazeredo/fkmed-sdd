package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: an appointment was rescheduled (SPEC-0009 §Events, BR10). Published inside the
 * reschedule transaction; the notification module's listener (wired at integration) delivers the
 * notice AFTER_COMMIT. Same frozen shape as {@link AppointmentConfirmed}; {@code protocol} is
 * unchanged and {@code scheduledAt} is the new instant.
 *
 * @param specialtyOrExamCode the specialty code (consultation) or exam-type code (exam).
 * @param scheduledAt the new real instant of the appointment.
 * @param authorAccountId the account that rescheduled it, may be {@code null}.
 */
public record AppointmentRescheduled(
    UUID appointmentId,
    UUID beneficiaryId,
    String protocol,
    String type,
    String specialtyOrExamCode,
    UUID unitId,
    Instant scheduledAt,
    UUID authorAccountId) {}
