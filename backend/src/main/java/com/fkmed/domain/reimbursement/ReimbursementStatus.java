package com.fkmed.domain.reimbursement;

/**
 * Lifecycle of a reimbursement request (SPEC-0015 BR13; the full state machine is SPEC-0016 BR1) —
 * a state machine, so per DECISIONS-BASELINE §0019 it is a genuine enum. This slice (6.1) only ever
 * writes {@link #EM_ANALISE} at submission; SPEC-0016 adds the remaining values and transitions
 * (altering this type/the {@code reimbursement_request.status} check constraint).
 */
public enum ReimbursementStatus {
  EM_ANALISE,
  PROCESSAMENTO,
  PENDENTE_DOCUMENTACAO,
  APROVADO,
  PAGO,
  PAGAMENTO_NAO_EFETUADO,
  NEGADO,
  CANCELADO;

  boolean isFinal() {
    return this == PAGO || this == NEGADO || this == CANCELADO;
  }
}
