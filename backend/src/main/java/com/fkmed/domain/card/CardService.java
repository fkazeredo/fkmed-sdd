package com.fkmed.domain.card;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.CardDetails;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the card module (SPEC-0007): the visual card + data sheet and its PDF.
 * Family scope and active status are decided by {@link BeneficiaryAccess#cardDetailsFor}
 * (SPEC-0003's facade, reused rather than reimplemented — ADR-0010). Viewing a dependent's card is
 * audited (BR7) in the same transaction as the read (BR4).
 */
@Service
@RequiredArgsConstructor
public class CardService {

  private final BeneficiaryAccess beneficiaryAccess;
  private final IdentityAccounts identityAccounts;
  private final AuditRecorder auditRecorder;

  /**
   * The card + data sheet of {@code targetBeneficiaryId} within the caller's family scope.
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when out of scope (404).
   * @throws CardUnavailableException when the beneficiary is inactive in the plan (409, BR10).
   */
  @Transactional
  public CardResponse cardFor(
      String beneficiaryCard,
      String authorEmail,
      UUID targetBeneficiaryId,
      AuditContext auditContext) {
    CardDetails details = beneficiaryAccess.cardDetailsFor(beneficiaryCard, targetBeneficiaryId);
    if (!details.active()) {
      throw new CardUnavailableException();
    }
    if (details.viewedAsDependent()) {
      auditRecorder.record(
          new AuditEntry(
              AuditEventTypes.DEPENDENT_CARD_VIEWED,
              authorAccountIdFor(authorEmail),
              targetBeneficiaryId,
              Map.of(),
              auditContext));
    }
    return CardResponse.from(details);
  }

  /**
   * The same card rendered as a downloadable PDF (BR3 fields, card-laid-on-A4 layout — DL-0009).
   * Reuses {@link #cardFor} so the scope/active decision and the BR7 audit happen exactly once, the
   * same way for both endpoints.
   */
  @Transactional
  public byte[] cardPdfFor(
      String beneficiaryCard,
      String authorEmail,
      UUID targetBeneficiaryId,
      AuditContext auditContext) {
    CardResponse card = cardFor(beneficiaryCard, authorEmail, targetBeneficiaryId, auditContext);
    return CardPdfRenderer.render(card);
  }

  private UUID authorAccountIdFor(String email) {
    return identityAccounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }
}
