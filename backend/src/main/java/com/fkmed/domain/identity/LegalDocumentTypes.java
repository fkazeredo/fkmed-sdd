package com.fkmed.domain.identity;

/**
 * Legal-document type codes (baseline §0019 — String codes, not an enum). Two code spaces meet
 * here: the public API/catalogue codes ({@link #TERMS}/{@link #PRIVACY}, used on the {@code
 * /api/legal-documents} paths and in the {@code legal_document} table) and the acceptance codes
 * ({@link #TERMS_OF_USE}/{@link #PRIVACY_POLICY}) recorded in {@code term_acceptance} since
 * SPEC-0002 BR15. {@link #acceptanceCodeFor} maps API → acceptance so first-access acceptances and
 * SPEC-0006 portal re-acceptances share one immutable history.
 */
public final class LegalDocumentTypes {

  /** Terms of Use — acceptance code (term_acceptance). */
  public static final String TERMS_OF_USE = "TERMS_OF_USE";

  /** Privacy Policy — acceptance code (term_acceptance). */
  public static final String PRIVACY_POLICY = "PRIVACY_POLICY";

  /** Terms of Use — public API/catalogue code (legal_document, request paths). */
  public static final String TERMS = "TERMS";

  /** Privacy Notice — public API/catalogue code (legal_document, request paths). */
  public static final String PRIVACY = "PRIVACY";

  private LegalDocumentTypes() {}

  /**
   * The {@code term_acceptance} document-type code for a public API/catalogue type.
   *
   * @throws IllegalArgumentException for a type other than {@link #TERMS}/{@link #PRIVACY} (the
   *     controller path already restricts the value, so this is a guard, not a user-facing error).
   */
  public static String acceptanceCodeFor(String apiType) {
    return switch (apiType) {
      case TERMS -> TERMS_OF_USE;
      case PRIVACY -> PRIVACY_POLICY;
      default -> throw new IllegalArgumentException("unknown legal document type: " + apiType);
    };
  }
}
