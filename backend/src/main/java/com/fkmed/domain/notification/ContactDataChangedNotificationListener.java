package com.fkmed.domain.notification;

import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.ContactDataChanged;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Wires SPEC-0006 to SPEC-0004: turns the plan module's {@link ContactDataChanged} into the
 * mandatory {@code account.contact-changed} notification — one in-app item for the owning account
 * plus a security-notice e-mail to the old and the new address ({@link
 * Notifications#notifyContactChange}). Runs AFTER_COMMIT in its own transaction ({@link
 * TransactionalEventListener} + {@code REQUIRES_NEW}, the core-Spring composition of Modulith's
 * {@code @ApplicationModuleListener}), so a delivery failure never rolls back the contact update
 * (BR6). The target account is resolved from the beneficiary via {@link IdentityAccounts}; when the
 * beneficiary has no account (a minor — DL-0006 routes to the titular), nothing is delivered here
 * because contact edits are self-service only (SPEC-0006 AC4), so a contact change always targets
 * an account-holding beneficiary. Title/body carry no sensitive data (BR4).
 */
@Component
@RequiredArgsConstructor
public class ContactDataChangedNotificationListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /** Notifies the beneficiary's account in-app and e-mails the old and new contact addresses. */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onContactDataChanged(ContactDataChanged event) {
    accounts
        .accountIdForBeneficiary(event.beneficiaryId())
        .ifPresent(
            accountId -> {
              String title =
                  messageSource.getMessage(
                      "notification.account.contact-changed.title", null, PRODUCT_LOCALE);
              String body =
                  messageSource.getMessage(
                      "notification.account.contact-changed.body", null, PRODUCT_LOCALE);
              notifications.notifyContactChange(
                  accountId, title, body, event.oldEmail(), event.newEmail());
            });
  }
}
