package com.fkmed.domain.notification;

import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.reimbursement.PreviewConcluded;
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

/** Turns concluded reimbursement previews into beneficiary notifications. */
@Component
@RequiredArgsConstructor
public class PreviewConcludedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onPreviewConcluded(PreviewConcluded event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.beneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String amount = NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(event.estimatedValue());
    notifications.notify(
        new NotificationRequest(
            account.get(),
            NotificationEventTypes.PREVIEW_CONCLUDED,
            messageSource.getMessage("notification.preview.concluded.title", null, PRODUCT_LOCALE),
            messageSource.getMessage(
                "notification.preview.concluded.body",
                new Object[] {event.protocol(), amount},
                PRODUCT_LOCALE),
            "/reembolso/previas/" + event.previewId(),
            accounts.contactEmailForBeneficiary(event.beneficiaryId()).orElse(null)));
  }
}
