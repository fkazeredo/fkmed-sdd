package com.fkmed.application.sim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Body of {@code POST /api/sim/tele/sessions/{id}/close} (SPEC-0018 BR5, SPEC-0010 BR9/BR10): the
 * closing professional's summary plus the documents to issue atomically at closure, bound to the
 * session's beneficiary and to the session as origin. {@code documents} may be empty/absent.
 */
public record CloseTeleSessionRequest(
    @NotBlank String professionalName,
    @NotBlank String crm,
    String guidance,
    @Valid List<SimDocumentSpec> documents) {

  /** The documents to issue, never {@code null}. */
  public List<SimDocumentSpec> documentsOrEmpty() {
    return documents == null ? List.of() : documents;
  }
}
