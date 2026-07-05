package com.fkmed.domain.card;

import com.fkmed.domain.plan.CardDetails;
import java.util.List;

/**
 * The digital card + data sheet (SPEC-0007 BR1, BR9): card number (9 digits), CNS in full (15
 * digits — the BR8 exception), ANS registration (6 digits), coverage (the BR2 seal) and the plan's
 * category/name/additives. A response record that maps a cross-module view stays inside its own
 * domain module (the delivery layer is entity-free).
 */
public record CardResponse(
    String fullName,
    String cardNumber,
    String cns,
    String ansRegistration,
    String coverage,
    String planName,
    String planCategory,
    List<String> additives) {

  /** Maps the plan module's scope-checked projection into the card API shape. */
  public static CardResponse from(CardDetails details) {
    return new CardResponse(
        details.fullName(),
        details.cardNumber(),
        details.cns(),
        details.ansRegistration(),
        details.coverage(),
        details.planName(),
        details.planCategory(),
        details.additives());
  }
}
