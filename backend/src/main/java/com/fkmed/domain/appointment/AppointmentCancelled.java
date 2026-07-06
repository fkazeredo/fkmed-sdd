package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: an appointment was cancelled (SPEC-0009 §Events, BR9). Published inside the cancel
 * transaction; the notification module's listener (wired at integration) delivers the notice
 * AFTER_COMMIT. Same frozen shape as {@link AppointmentConfirmed}.
 *
 * @param specialtyOrExamCode the specialty code (consultation) or exam-type code (exam).
 * @param scheduledAt the real instant the appointment had been scheduled for.
 * @param authorAccountId the account that cancelled it, may be {@code null}.
 */
public record AppointmentCancelled(
    UUID appointmentId,
    UUID beneficiaryId,
    String protocol,
    String type,
    String specialtyOrExamCode,
    UUID unitId,
    Instant scheduledAt,
    UUID authorAccountId) {}
