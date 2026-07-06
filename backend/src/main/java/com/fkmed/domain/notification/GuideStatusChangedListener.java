package com.fkmed.domain.notification;

import com.fkmed.domain.guides.GuideStatusChanged;
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
 * Wires SPEC-0012 to SPEC-0004: turns the guides module's {@link GuideStatusChanged} (an
 * operator-driven transition, SPEC-0018) into the {@code guide.status-changed} notification — one
 * in-app item for the owning account plus (business, opt-outable) an e-mail. Runs AFTER_COMMIT in
 * its own transaction ({@code REQUIRES_NEW}) so a delivery failure never rolls back the guide
 * transition; mirrors {@link TeleNotificationListener}. Carries only the guide number, the new
 * status and — when denied — the reason (BR8): never item/clinical detail.
 */
@Component
@RequiredArgsConstructor
public class GuideStatusChangedListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final String GUIDES_LINK = "/guias";

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /** Notifies the beneficiary's account that a guide's status changed (BR8). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onGuideStatusChanged(GuideStatusChanged event) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(event.beneficiaryId());
    if (account.isEmpty()) {
      return;
    }
    String statusLabel =
        messageSource.getMessage(
            "notification.guide.status." + event.newStatus().name(),
            null,
            event.newStatus().name(),
            PRODUCT_LOCALE);
    String title =
        messageSource.getMessage("notification.guide.status-changed.title", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "notification.guide.status-changed.body",
            new Object[] {event.number(), statusLabel},
            PRODUCT_LOCALE);
    if (event.denialReason() != null && !event.denialReason().isBlank()) {
      body +=
          " "
              + messageSource.getMessage(
                  "notification.guide.status-changed.denial-reason",
                  new Object[] {event.denialReason()},
                  PRODUCT_LOCALE);
    }
    String recipientEmail = accounts.contactEmailForBeneficiary(event.beneficiaryId()).orElse(null);
    notifications.notify(
        new NotificationRequest(
            account.get(),
            NotificationEventTypes.GUIDE_STATUS_CHANGED,
            title,
            body,
            GUIDES_LINK,
            recipientEmail));
  }
}
