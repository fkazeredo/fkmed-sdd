package com.fkmed.domain.identity;

/**
 * Legal-document type codes recorded in {@code term_acceptance} (SPEC-0002 BR15). String codes, not
 * an enum (baseline §0019); the documents themselves — pages and new-version re-acceptance — are
 * owned by SPEC-0006 (this slice only records acceptance of the current versions, DL-0001).
 */
public final class LegalDocumentTypes {

  /** Terms of Use. */
  public static final String TERMS_OF_USE = "TERMS_OF_USE";

  /** Privacy Policy. */
  public static final String PRIVACY_POLICY = "PRIVACY_POLICY";

  private LegalDocumentTypes() {}
}
