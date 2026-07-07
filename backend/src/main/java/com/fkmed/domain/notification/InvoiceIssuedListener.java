package com.fkmed.domain.notification;

import com.fkmed.domain.finance.InvoiceIssued;
import com.fkmed.domain.identity.IdentityAccounts;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
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
 * Wires SPEC-0013 to SPEC-0004: turns the finance module's {@link InvoiceIssued} (an operator sim
 * issuance, SPEC-0018) into the {@code finance.invoice-issued} notification — one in-app item for
 * the titular's account plus (business, opt-outable) an e-mail — carrying the competência, amount
 * and due date with a link to the boleto. Runs AFTER_COMMIT in its own transaction ({@code
 * REQUIRES_NEW}) so a delivery failure never rolls back the issuance; mirrors {@link
 * ClinicalDocumentIssuedListener}. The event carries NO sensitive payment identifier (BR — the
 * digitable line / PIX code are never logged), so nothing sensitive reaches the notification.
 */
@Component
@RequiredArgsConstructor
public class InvoiceIssuedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", PRODUCT_LOCALE);

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /** Notifies the titular's account that a new invoice is available (SPEC-0013 §Events). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onInvoiceIssued(InvoiceIssued event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.titularBeneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String amount = NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(event.amount());
    String dueDate = DATE.format(event.dueDate());
    String title =
        messageSource.getMessage("notification.finance.invoice-issued.title", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "notification.finance.invoice-issued.body",
            new Object[] {event.competencia(), amount, dueDate},
            PRODUCT_LOCALE);
    String recipientEmail =
        accounts.contactEmailForBeneficiary(event.titularBeneficiaryId()).orElse(null);
    notifications.notify(
        new NotificationRequest(
            account.get(),
            NotificationEventTypes.FINANCE_INVOICE_ISSUED,
            title,
            body,
            "/financas/boleto/" + event.invoiceId(),
            recipientEmail));
  }
}
