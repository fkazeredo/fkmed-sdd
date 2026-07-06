package com.fkmed.application.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * The new slot for a reschedule (SPEC-0009 BR10): only the date/time reopens; beneficiary,
 * specialty/exam, unit and protocol are kept from the existing appointment.
 *
 * @param slot the new clinic-local date+time of the slot (ISO, e.g. {@code 2026-07-12T14:30}).
 */
public record RescheduleAppointmentRequest(@NotNull LocalDateTime slot) {}
