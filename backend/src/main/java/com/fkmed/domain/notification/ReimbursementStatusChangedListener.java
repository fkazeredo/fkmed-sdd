package com.fkmed.domain.notification;

import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.reimbursement.ReimbursementStatus;
import com.fkmed.domain.reimbursement.ReimbursementStatusChanged;
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

/** Turns reimbursement lifecycle events into SPEC-0004 notifications. */
@Component
@RequiredArgsConstructor
public class ReimbursementStatusChangedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onReimbursementStatusChanged(ReimbursementStatusChanged event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.beneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String type = typeOf(event.status());
    if (type == null) {
      return;
    }
    String key = keyOf(event.status());
    String title = messageSource.getMessage(key + ".title", null, PRODUCT_LOCALE);
    String body = messageSource.getMessage(key + ".body", argsOf(event), PRODUCT_LOCALE);
    notifications.notify(
        new NotificationRequest(
            account.get(),
            type,
            title,
            body,
            "/reembolso/" + event.requestId(),
            accounts.contactEmailForBeneficiary(event.beneficiaryId()).orElse(null)));
  }

  private String typeOf(ReimbursementStatus status) {
    return switch (status) {
      case PENDENTE_DOCUMENTACAO -> NotificationEventTypes.REIMBURSEMENT_PENDENCY_OPENED;
      case PROCESSAMENTO -> NotificationEventTypes.REIMBURSEMENT_PENDENCY_RESOLVED;
      case APROVADO -> NotificationEventTypes.REIMBURSEMENT_APPROVED;
      case PAGO -> NotificationEventTypes.REIMBURSEMENT_PAID;
      case PAGAMENTO_NAO_EFETUADO -> NotificationEventTypes.REIMBURSEMENT_PAYMENT_FAILED;
      case NEGADO -> NotificationEventTypes.REIMBURSEMENT_DENIED;
      case CANCELADO -> NotificationEventTypes.REIMBURSEMENT_CANCELLED;
      case EM_ANALISE -> null;
    };
  }

  private String keyOf(ReimbursementStatus status) {
    return switch (status) {
      case PENDENTE_DOCUMENTACAO -> "notification.reimbursement.pendency-opened";
      case PROCESSAMENTO -> "notification.reimbursement.pendency-resolved";
      case APROVADO -> "notification.reimbursement.approved";
      case PAGO -> "notification.reimbursement.paid";
      case PAGAMENTO_NAO_EFETUADO -> "notification.reimbursement.payment-failed";
      case NEGADO -> "notification.reimbursement.denied";
      case CANCELADO -> "notification.reimbursement.cancelled";
      case EM_ANALISE -> "notification.reimbursement.submitted";
    };
  }

  private Object[] argsOf(ReimbursementStatusChanged event) {
    String value =
        NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(event.amountReimbursed());
    return switch (event.status()) {
      case PENDENTE_DOCUMENTACAO, NEGADO -> new Object[] {event.protocol(), event.reason()};
      case APROVADO -> new Object[] {event.protocol(), value};
      case PAGO -> new Object[] {event.protocol(), value, event.maskedBankAccount()};
      default -> new Object[] {event.protocol()};
    };
  }
}
