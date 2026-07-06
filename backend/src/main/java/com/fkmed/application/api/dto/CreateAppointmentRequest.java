package com.fkmed.application.api.dto;

import com.fkmed.domain.appointment.AppointmentType;
import com.fkmed.domain.appointment.BookAppointmentCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JSON confirmation of a booking without an attachment — the consultation flow (SPEC-0009 §I/O
 * Examples). The active beneficiary and the author are resolved server-side, never trusted from the
 * body beyond the scope-checked {@code beneficiaryId}.
 *
 * @param slot the clinic-local date+time of the slot (ISO, e.g. {@code 2026-07-10T09:00}).
 */
public record CreateAppointmentRequest(
    @NotNull UUID beneficiaryId,
    @NotNull AppointmentType type,
    String specialty,
    String exam,
    @NotNull UUID unitId,
    @NotNull LocalDateTime slot) {

  /** Maps to the domain command, stamping the resolved caller card and author account. */
  public BookAppointmentCommand toCommand(String callerCard, UUID authorAccountId) {
    return new BookAppointmentCommand(
        callerCard, authorAccountId, beneficiaryId, type, specialty, exam, unitId, slot);
  }
}
