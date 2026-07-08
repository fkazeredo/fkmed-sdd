package com.fkmed.domain.reimbursement;

/**
 * Brazilian CPF/CNPJ check-digit validation (SPEC-0015 BR10: "valid CPF or CNPJ"). Deliberately a
 * small, self-contained mod-11 implementation local to this module — mirrors {@code
 * domain.identity.CpfCheckDigits} (that module's private invariant) rather than reaching across the
 * module boundary for it (backend.md: prefer small duplication over a bad shared abstraction; no
 * {@code shared} module — baseline §0012). Unlike the identity module's CPF check (a first-access
 * neutrality concern, BR1), a provider document failing this check is a plain, direct 422
 * (SPEC-0015 has no oracle-avoidance requirement here).
 */
final class DocumentCheckDigits {

  private static final int[] CNPJ_WEIGHTS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final int[] CNPJ_WEIGHTS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

  private DocumentCheckDigits() {}

  /** True when {@code cpf} is 11 digits, not all-repeated, and both check digits are correct. */
  static boolean isValidCpf(String cpf) {
    if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
      return false;
    }
    return cpfCheckDigit(cpf, 9) == cpf.charAt(9) - '0'
        && cpfCheckDigit(cpf, 10) == cpf.charAt(10) - '0';
  }

  /** True when {@code cnpj} is 14 digits, not all-repeated, and both check digits are correct. */
  static boolean isValidCnpj(String cnpj) {
    if (cnpj == null || !cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
      return false;
    }
    int digit1 = cnpjCheckDigit(cnpj, 12, CNPJ_WEIGHTS_1);
    if (digit1 != cnpj.charAt(12) - '0') {
      return false;
    }
    int digit2 = cnpjCheckDigit(cnpj, 13, CNPJ_WEIGHTS_2);
    return digit2 == cnpj.charAt(13) - '0';
  }

  /** True when {@code document} is a valid 11-digit CPF or a valid 14-digit CNPJ. */
  static boolean isValid(String document) {
    if (document == null) {
      return false;
    }
    return document.length() == 11 ? isValidCpf(document) : isValidCnpj(document);
  }

  private static int cpfCheckDigit(String cpf, int length) {
    int sum = 0;
    for (int i = 0; i < length; i++) {
      sum += (cpf.charAt(i) - '0') * (length + 1 - i);
    }
    int remainder = (sum * 10) % 11;
    return remainder == 10 ? 0 : remainder;
  }

  private static int cnpjCheckDigit(String cnpj, int length, int[] weights) {
    int sum = 0;
    for (int i = 0; i < length; i++) {
      sum += (cnpj.charAt(i) - '0') * weights[i];
    }
    int remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  }
}
