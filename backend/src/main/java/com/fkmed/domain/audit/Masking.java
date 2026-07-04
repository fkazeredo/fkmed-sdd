package com.fkmed.domain.audit;

/**
 * Personal-data masking for audit details and logs (SPEC-0003 BR8): sensitive values are stored in
 * the trail only in masked form (CPF never in full, e-mail reduced to a hint). Callers pass values
 * through these helpers before handing them to {@link AuditRecorder}.
 */
public final class Masking {

  private Masking() {}

  /**
   * Masks an e-mail to its first character and domain (e.g. {@code maria@fkmed.local} → {@code
   * m***@fkmed.local}). Blank input yields an empty string.
   */
  public static String email(String email) {
    if (email == null || email.isBlank()) {
      return "";
    }
    int at = email.indexOf('@');
    if (at <= 0) {
      return "***";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }

  /**
   * Masks a CPF to its last two digits (e.g. {@code 52998224725} → {@code *********25}). Blank
   * input yields an empty string.
   */
  public static String cpf(String cpf) {
    if (cpf == null || cpf.isBlank()) {
      return "";
    }
    if (cpf.length() <= 2) {
      return "**";
    }
    return "*".repeat(cpf.length() - 2) + cpf.substring(cpf.length() - 2);
  }
}
