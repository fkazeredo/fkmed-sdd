package com.fkmed.domain.notification;

import com.fkmed.domain.appointment.AppointmentCancelled;
import com.fkmed.domain.appointment.AppointmentConfirmed;
import com.fkmed.domain.appointment.AppointmentRescheduled;
import com.fkmed.domain.identity.IdentityAccounts;
import java.time.Instant;
import java.time.ZoneId;
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
 * Wires SPEC-0009 to SPEC-0004: turns the appointment module's {@link AppointmentConfirmed}, {@link
 * AppointmentCancelled} and {@link AppointmentRescheduled} events into the {@code
 * appointment.confirmed}/{@code .cancelled}/{@code .rescheduled} notifications — one in-app item
 * for the owning account plus (for these e-mail-defaulting, opt-outable types) an e-mail to the
 * account's contact address. Runs AFTER_COMMIT in its own transaction ({@link
 * TransactionalEventListener} + {@code REQUIRES_NEW}, the core-Spring composition of Modulith's
 * {@code @ApplicationModuleListener}), so a delivery failure never rolls back the booking. Mirrors
 * {@link ContactDataChangedNotificationListener}. The owning account is resolved from the
 * beneficiary via {@link IdentityAccounts}; when the beneficiary has no account (a minor — DL-0006)
 * nothing is delivered here. Title/body carry no sensitive data (BR4): only protocol,
 * specialty/exam code and date/time — the frozen events carry no name, so full details live behind
 * the portal link.
 */
@Component
@RequiredArgsConstructor
public class AppointmentNotificationListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", PRODUCT_LOCALE);
  private static final String LINK = "/agendamentos";

  private final Notifications notifications;
  private final IdentityAccounts accounts;
  private final MessageSource messageSource;

  /** Notifies the beneficiary's account that a booking was confirmed (BR7). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onAppointmentConfirmed(AppointmentConfirmed event) {
    deliver(
        event.beneficiaryId(),
        NotificationEventTypes.APPOINTMENT_CONFIRMED,
        "notification.appointment.confirmed",
        event.protocol(),
        event.type(),
        event.specialtyOrExamCode(),
        event.scheduledAt());
  }

  /** Notifies the beneficiary's account that an appointment was cancelled (BR9). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onAppointmentCancelled(AppointmentCancelled event) {
    deliver(
        event.beneficiaryId(),
        NotificationEventTypes.APPOINTMENT_CANCELLED,
        "notification.appointment.cancelled",
        event.protocol(),
        event.type(),
        event.specialtyOrExamCode(),
        event.scheduledAt());
  }

  /** Notifies the beneficiary's account that an appointment was rescheduled (BR10). */
  @TransactionalEventListener
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onAppointmentRescheduled(AppointmentRescheduled event) {
    deliver(
        event.beneficiaryId(),
        NotificationEventTypes.APPOINTMENT_RESCHEDULED,
        "notification.appointment.rescheduled",
        event.protocol(),
        event.type(),
        event.specialtyOrExamCode(),
        event.scheduledAt());
  }

  private void deliver(
      UUID beneficiaryId,
      String eventTypeCode,
      String messageKeyPrefix,
      String protocol,
      String type,
      String specialtyOrExamCode,
      Instant scheduledAt) {
    Optional<UUID> account = accounts.accountIdForBeneficiary(beneficiaryId);
    if (account.isEmpty()) {
      return;
    }
    String kind =
        messageSource.getMessage(
            "notification.appointment.kind." + type, null, type, PRODUCT_LOCALE);
    Object[] args = {
      protocol, kind, specialtyOrExamCode, WHEN.format(scheduledAt.atZone(CLINIC_ZONE))
    };
    String title = messageSource.getMessage(messageKeyPrefix + ".title", null, PRODUCT_LOCALE);
    String body = messageSource.getMessage(messageKeyPrefix + ".body", args, PRODUCT_LOCALE);
    String recipientEmail = accounts.contactEmailForBeneficiary(beneficiaryId).orElse(null);
    notifications.notify(
        new NotificationRequest(account.get(), eventTypeCode, title, body, LINK, recipientEmail));
  }
}
