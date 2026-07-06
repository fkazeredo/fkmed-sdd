package com.fkmed.domain.appointment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The input to a booking (SPEC-0009 §I/O Examples): who is booking (resolved caller card + author
 * account), for whom ({@code beneficiaryId}, scope-checked), what ({@code type} + specialty or exam
 * code), where ({@code unitId}) and when ({@code slot} as the clinic-local date+time, e.g. {@code
 * 2026-07-10T09:00}). The medical-order bytes travel separately (multipart), never in this command.
 */
public record BookAppointmentCommand(
    String callerCard,
    UUID authorAccountId,
    UUID beneficiaryId,
    AppointmentType type,
    String specialtyCode,
    String examCode,
    UUID unitId,
    LocalDateTime slot) {}
