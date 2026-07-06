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
 * <p>{@code telemedicine} is the SPEC-0010 BR14 scheduled-teleconsultation scope (the merged FE
 * books with it and NO {@code unitId} — DL-0018): the controller resolves the virtual Telemedicina
 * unit server-side, so exactly one of {@code telemedicine=true} or an explicit {@code unitId} is
 * required.
 *
 * @param slot the clinic-local date+time of the slot (ISO, e.g. {@code 2026-07-10T09:00}).
 */
public record CreateAppointmentRequest(
    @NotNull UUID beneficiaryId,
    @NotNull AppointmentType type,
    String specialty,
    String exam,
    UUID unitId,
    Boolean telemedicine,
    @NotNull LocalDateTime slot) {

  /** Whether this is a scheduled teleconsultation (nullable/absent on the wire ⇒ false). */
  public boolean isTelemedicine() {
    return Boolean.TRUE.equals(telemedicine);
  }

  /** Maps to the domain command with the server-resolved {@code unitId} (virtual unit for tele). */
  public BookAppointmentCommand toCommand(
      String callerCard, UUID authorAccountId, UUID resolvedUnitId) {
    return new BookAppointmentCommand(
        callerCard, authorAccountId, beneficiaryId, type, specialty, exam, resolvedUnitId, slot);
  }
}
