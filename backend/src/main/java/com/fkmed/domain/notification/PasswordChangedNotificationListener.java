package com.fkmed.domain.notification;

import com.fkmed.domain.identity.PasswordChanged;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns identity's {@link PasswordChanged} into the {@code account.password-changed} in-app
 * notification (SPEC-0004; DL-0008). Runs AFTER_COMMIT in its own transaction ({@link
 * TransactionalEventListener} + {@code REQUIRES_NEW} — the core-Spring composition of Modulith's
 * {@code @ApplicationModuleListener}, which lives in the optional {@code spring-modulith-events}
 * starter not on this classpath), so a failure here never rolls back the password change. The type
 * is seeded {@code email_default=false} (DL-0008): the module creates only the in-app item — the
 * existing identity e-mail seam already sends the "se não foi você" notice, so there is no
 * double-send. The pt-BR title/body carry no sensitive data (BR4).
 */
@Component
@RequiredArgsConstructor
public class PasswordChangedNotificationListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final Notifications notifications;
  private final MessageSource messageSource;

  /** Creates the in-app account.password-changed notification for the account. */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onPasswordChanged(PasswordChanged event) {
    String title =
        messageSource.getMessage(
            "notification.account.password-changed.title", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "notification.account.password-changed.body", null, PRODUCT_LOCALE);
    notifications.notify(
        new NotificationRequest(
            event.accountId(),
            NotificationEventTypes.ACCOUNT_PASSWORD_CHANGED,
            title,
            body,
            null,
            event.email()));
  }
}
