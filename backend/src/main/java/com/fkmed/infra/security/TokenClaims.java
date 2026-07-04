package com.fkmed.infra.security;

/** Custom JWT claim names issued by the embedded Authorization Server. */
public final class TokenClaims {

  /** Beneficiary card number bound to the authenticated user (SPEC-0001 BR8 dev seam). */
  public static final String BENEFICIARY_CARD = "beneficiary_card";

  private TokenClaims() {}
}
