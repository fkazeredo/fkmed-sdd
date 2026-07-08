package com.fkmed.domain.reimbursement;

/**
 * Fixed bank-account types (SPEC-0015 BR11) — a technical classification fixed by the domain (no
 * operator editing), so per DECISIONS-BASELINE §0019 a plain enum, not a registry code.
 */
public enum BankAccountType {
  CORRENTE,
  POUPANCA
}
