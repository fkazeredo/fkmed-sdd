package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event: an appointment was confirmed (SPEC-0009 §Events, BR7). Published inside the booking
 * transaction; the notification module's listener (wired at integration — ADR-0012) delivers the
 * in-app + e-mail notice AFTER_COMMIT. This module only publishes it and never handles delivery.
 *
 * @param specialtyOrExamCode the specialty code (consultation) or exam-type code (exam).
 * @param scheduledAt the real instant of the slot.
 * @param authorAccountId the account that made the booking (SPEC-0003 author), may be {@code null}.
 */
public record AppointmentConfirmed(
    UUID appointmentId,
    UUID beneficiaryId,
    String protocol,
    String type,
    String specialtyOrExamCode,
    UUID unitId,
    Instant scheduledAt,
    UUID authorAccountId) {}
