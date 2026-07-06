package com.fkmed.domain.notification;

import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.telemedicine.TeleSessionClosed;
import com.fkmed.domain.telemedicine.TeleTurnReached;
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
 * Wires SPEC-0010 to SPEC-0004: turns the telemedicine module's {@link TeleTurnReached} (your turn,
 * BR8) and {@link TeleSessionClosed} (session closed with its summary + documents, BR9) into the
 * {@code tele.turn-reached}/{@code tele.session-closed} notifications — one in-app item for the
 * owning account plus (for these e-mail-defaulting, opt-outable types) an e-mail. Runs AFTER_COMMIT
 * in its own transaction ({@code REQUIRES_NEW}), so a delivery failure never rolls back the tele
 * transition. Mirrors {@link AppointmentNotificationListener}. The owning account is resolved from
 * the attended beneficiary via {@link IdentityAccounts} (nothing delivered when the beneficiary has
 * no account — a minor). No clinical content rides the notification (BR4 / SPEC-0010
 * §Observability: the complaint/guidance are sensitive): the deep link carries the user to the
 * room/summary.
 */
@Component
@RequiredArgsConstructor
public class TeleNotificationListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final String ROOM_LINK = "/telemedicina/sessao";
  private static final String MINHA_SAUDE_LINK = "/minha-saude";

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /** Notifies the beneficiary's account that it is their turn and the room is open (BR8). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onTurnReached(TeleTurnReached event) {
    deliver(
        event.beneficiaryId(),
        NotificationEventTypes.TELE_TURN_REACHED,
        "notification.tele.turn-reached",
        ROOM_LINK);
  }

  /** Notifies the beneficiary's account that the session closed with its summary (BR9). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onSessionClosed(TeleSessionClosed event) {
    deliver(
        event.beneficiaryId(),
        NotificationEventTypes.TELE_SESSION_CLOSED,
        "notification.tele.session-closed",
        MINHA_SAUDE_LINK);
  }

  private void deliver(
      UUID beneficiaryId, String eventTypeCode, String messageKeyPrefix, String link) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(beneficiaryId);
    if (account.isEmpty()) {
      return;
    }
    String title = messageSource.getMessage(messageKeyPrefix + ".title", null, PRODUCT_LOCALE);
    String body = messageSource.getMessage(messageKeyPrefix + ".body", null, PRODUCT_LOCALE);
    String recipientEmail = accounts.contactEmailForBeneficiary(beneficiaryId).orElse(null);
    notifications.notify(
        new NotificationRequest(account.get(), eventTypeCode, title, body, link, recipientEmail));
  }
}
