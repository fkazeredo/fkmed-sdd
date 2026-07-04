package com.fkmed.domain.error;

/**
 * Base class of every business error (domain kernel — DECISIONS-BASELINE §0011).
 *
 * <p>Carries only domain data: a stable {@code code} (which doubles as the i18n message key) and
 * optional message arguments. No transport concern (no HTTP status, headers or response shape) —
 * the HTTP mapping lives in {@code com.fkmed.infra.web.HttpErrorMapping}.
 *
 * <p>Convention (enforced by ArchUnit + the i18n completeness gate): every concrete subclass
 * declares a {@code public static final String CODE} constant with its stable code, and every code
 * has a message in the product locale bundle.
 */
public abstract class DomainException extends RuntimeException {

  private final String code;
  private final transient Object[] args;

  protected DomainException(String code, Object... args) {
    super(code);
    this.code = code;
    this.args = args;
  }

  public String getCode() {
    return code;
  }

  /** Message arguments interpolated into the i18n message; never {@code null}. */
  public Object[] getArgs() {
    return args.clone();
  }
}
