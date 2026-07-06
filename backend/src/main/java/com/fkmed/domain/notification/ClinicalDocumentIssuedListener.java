package com.fkmed.domain.notification;

import com.fkmed.domain.clinicaldocs.ClinicalDocumentIssued;
import com.fkmed.domain.identity.IdentityAccounts;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Wires SPEC-0011 to SPEC-0004: turns the clinical-documents module's {@link
 * ClinicalDocumentIssued} (a tele closure or operator issuance, SPEC-0010 BR10 / SPEC-0018) into
 * the {@code clinical-document.issued} notification — one in-app item for the owning account plus
 * (business, opt-outable) an e-mail — carrying the "Ver em Minha Saúde" link (SPEC-0011 AC4). Runs
 * AFTER_COMMIT in its own transaction ({@code REQUIRES_NEW}) so a delivery failure never rolls back
 * the issuance; mirrors {@link AppointmentNotificationListener}. The document type drives a
 * non-sensitive kind label (BR4 — no clinical content in the notification); the full document lives
 * behind the link.
 */
@Component
@RequiredArgsConstructor
public class ClinicalDocumentIssuedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final String MINHA_SAUDE_LINK = "/minha-saude";

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /**
   * Notifies the beneficiary's account that a new document is available in Minha Saúde (BR3/AC4).
   */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onClinicalDocumentIssued(ClinicalDocumentIssued event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.beneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String kind =
        messageSource.getMessage(
            "notification.clinical-document.kind." + event.type().name(),
            null,
            event.type().name(),
            PRODUCT_LOCALE);
    String title =
        messageSource.getMessage(
            "notification.clinical-document.issued.title", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "notification.clinical-document.issued.body", new Object[] {kind}, PRODUCT_LOCALE);
    String recipientEmail = accounts.contactEmailForBeneficiary(event.beneficiaryId()).orElse(null);
    notifications.notify(
        new NotificationRequest(
            account.get(),
            NotificationEventTypes.CLINICAL_DOCUMENT_ISSUED,
            title,
            body,
            MINHA_SAUDE_LINK,
            recipientEmail));
  }
}
