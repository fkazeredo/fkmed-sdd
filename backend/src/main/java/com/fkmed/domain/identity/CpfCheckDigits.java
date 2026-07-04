package com.fkmed.domain.identity;

/**
 * Brazilian CPF check-digit validation (SPEC-0002 §Validation Rules: "11 digits, valid check
 * digits"). Deliberately a small, self-contained mod-11 implementation local to this module —
 * mirrors {@code domain.plan.Beneficiary}'s own CPF check (that module's private invariant) rather
 * than reaching across the module boundary for it (backend.md: prefer small duplication over a bad
 * shared abstraction; no {@code shared} module — baseline §0012).
 *
 * <p><b>Callers MUST treat an invalid CPF exactly like a beneficiary-triple mismatch</b> (BR1
 * neutrality): never surface a distinct "invalid CPF" error, or the check-digit failure becomes an
 * oracle revealing which field diverged.
 */
final class CpfCheckDigits {

  private CpfCheckDigits() {}

  /** True when {@code cpf} is 11 digits, not all-repeated, and both check digits are correct. */
  static boolean isValid(String cpf) {
    if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
      return false;
    }
    return checkDigit(cpf, 9) == cpf.charAt(9) - '0' && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
  }

  private static int checkDigit(String cpf, int length) {
    int sum = 0;
    for (int i = 0; i < length; i++) {
      sum += (cpf.charAt(i) - '0') * (length + 1 - i);
    }
    int remainder = (sum * 10) % 11;
    return remainder == 10 ? 0 : remainder;
  }
}
