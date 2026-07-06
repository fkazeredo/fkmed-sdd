package com.fkmed.domain.clinicaldocs;

import java.util.UUID;

/**
 * Where a clinical document was born (SPEC-0011 §Persistence Changes: "origin session/operator
 * ref"): a telemedicine session close (SPEC-0010 BR10) or an operator action (SPEC-0018), never a
 * beneficiary write path (BR8). Exactly one of the two refs is set — enforced at construction and
 * mirrored by the {@code chk_clinical_document_origin} check constraint (V18).
 *
 * @param sessionId the telemedicine session that issued the document, or {@code null} when the
 *     origin is an operator action.
 * @param operatorAccountId the operator account that issued the document, or {@code null} when the
 *     origin is a telemedicine session.
 */
public record DocumentOrigin(UUID sessionId, UUID operatorAccountId) {

  public DocumentOrigin {
    if ((sessionId == null) == (operatorAccountId == null)) {
      throw new IllegalArgumentException(
          "exactly one of sessionId or operatorAccountId is required");
    }
  }

  /** A document issued at a telemedicine session close (SPEC-0010 BR10). */
  public static DocumentOrigin ofSession(UUID sessionId) {
    return new DocumentOrigin(sessionId, null);
  }

  /** A document issued by an operator action (SPEC-0018). */
  public static DocumentOrigin ofOperator(UUID operatorAccountId) {
    return new DocumentOrigin(null, operatorAccountId);
  }
}
