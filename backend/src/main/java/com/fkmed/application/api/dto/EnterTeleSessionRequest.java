package com.fkmed.application.api.dto;

import com.fkmed.domain.telemedicine.EnterQueueCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Triage + term acceptance to enter the Pronto Atendimento queue (SPEC-0010 POST
 * /api/tele/sessions, §I/O Examples). The caller card and author account are resolved server-side;
 * the attended {@code beneficiaryId} is scope-checked. The complaint length (BR2), the term version
 * (BR4) and the symptoms/duration (BR2) are validated in the domain so they surface as the stable
 * {@code tele.*} error codes rather than a generic bean-validation 400.
 */
public record EnterTeleSessionRequest(
    @NotNull UUID beneficiaryId,
    String complaint,
    List<String> symptoms,
    String otherSymptom,
    String duration,
    String termVersion) {

  /** Maps to the domain command, stamping the resolved caller card and author account. */
  public EnterQueueCommand toCommand(String callerCard, UUID authorAccountId) {
    return new EnterQueueCommand(
        callerCard,
        authorAccountId,
        beneficiaryId,
        complaint,
        symptoms == null ? List.of() : symptoms,
        otherSymptom,
        duration,
        termVersion);
  }
}
