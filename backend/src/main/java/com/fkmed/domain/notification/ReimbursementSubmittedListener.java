package com.fkmed.domain.notification;

import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.reimbursement.ReimbursementSubmitted;
import java.text.NumberFormat;
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
 * Wires SPEC-0015 to SPEC-0004: turns {@link ReimbursementSubmitted} into the {@code
 * reimbursement.submitted} notification — one in-app item for the requester's account plus
 * (business, opt-outable) an e-mail — carrying the protocol and expected payment date. Runs
 * AFTER_COMMIT in its own transaction ({@code REQUIRES_NEW}) so a delivery failure never rolls back
 * the submission; mirrors {@link InvoiceIssuedListener}. The event carries no provider/bank data.
 */
@Component
@RequiredArgsConstructor
public class ReimbursementSubmittedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /**
   * Notifies the requester's account that the reimbursement request was submitted (SPEC-0015
   * §Events).
   */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onReimbursementSubmitted(ReimbursementSubmitted event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.beneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String amount = NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(event.amount());
    String title =
        messageSource.getMessage(
            "notification.reimbursement.submitted.title", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "notification.reimbursement.submitted.body",
            new Object[] {event.protocol(), amount},
            PRODUCT_LOCALE);
    String recipientEmail = accounts.contactEmailForBeneficiary(event.beneficiaryId()).orElse(null);
    notifications.notify(
        new NotificationRequest(
            account.get(),
            NotificationEventTypes.REIMBURSEMENT_SUBMITTED,
            title,
            body,
            "/reembolso",
            recipientEmail));
  }
}
