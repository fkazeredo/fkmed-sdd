package com.fkmed.application.api.dto;

import com.fkmed.domain.reimbursement.SessionInput;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One Terapia/Psicologia session submitted in the reimbursement wizard (SPEC-0015 BR7). */
public record ReimbursementSessionRequest(
    @NotNull LocalDate sessionDate, @NotNull BigDecimal amount) {

  public SessionInput toInput() {
    return new SessionInput(sessionDate, amount);
  }
}
