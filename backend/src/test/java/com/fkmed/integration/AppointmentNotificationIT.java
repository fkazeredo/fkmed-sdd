package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.appointment.AppointmentCancelled;
import com.fkmed.domain.appointment.AppointmentConfirmed;
import com.fkmed.domain.appointment.AppointmentRescheduled;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cross-spec wiring SPEC-0009 §Events × SPEC-0004: each appointment event becomes the corresponding
 * in-app notification for the owning account and dispatches its e-mail AFTER_COMMIT (these types
 * are email_default=true, opt-outable). The account/e-mail are resolved from the beneficiary via
 * {@code IdentityAccounts}. Rows/mails are wiped per test for absolute-count isolation on the
 * shared Postgres.
 */
@Import(RecordingMailConfig.class)
class AppointmentNotificationIT extends AbstractIntegrationTest {

  @Autowired private ApplicationEventPublisher events;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;
  @Autowired private PlatformTransactionManager transactionManager;

  private UUID accountId;
  private UUID beneficiaryId;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification");
    mail.messages.clear();
  }

  @BeforeEach
  void resolveMaria() {
    Map<String, Object> maria =
        jdbc.queryForMap(
            "select id, beneficiary_id from user_account where email = ?", "maria@fkmed.local");
    accountId = UUID.fromString(maria.get("id").toString());
    beneficiaryId = UUID.fromString(maria.get("beneficiary_id").toString());
  }

  @Test
  void confirmedEvent_createsInAppItem_andEmailsTheAccountAddress() {
    publish(
        new AppointmentConfirmed(
            UUID.randomUUID(),
            beneficiaryId,
            "AG-20260706-0001",
            "CONSULTATION",
            "CARDIOLOGIA",
            UUID.randomUUID(),
            Instant.parse("2026-07-10T12:00:00Z"),
            accountId));

    await(() -> count() == 1 && mail.messages.size() == 1);

    Map<String, Object> row =
        jdbc.queryForMap(
            "select event_type_code, read_at, body from notification where account_id = ?",
            accountId);
    assertThat(row.get("event_type_code")).isEqualTo("appointment.confirmed");
    assertThat(row.get("read_at")).isNull();
    assertThat(row.get("body").toString()).contains("AG-20260706-0001");
    assertThat(mail.messages.getFirst().to()).isEqualTo("maria@fkmed.local");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("Agendamento confirmado");
  }

  @Test
  void cancelledEvent_createsInAppItem_andEmails() {
    publish(
        new AppointmentCancelled(
            UUID.randomUUID(),
            beneficiaryId,
            "AG-20260706-0002",
            "EXAM",
            "HEMOGRAMA",
            UUID.randomUUID(),
            Instant.parse("2026-07-10T12:00:00Z"),
            accountId));

    await(() -> count() == 1 && mail.messages.size() == 1);
    assertThat(eventTypeCode()).isEqualTo("appointment.cancelled");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("Agendamento cancelado");
  }

  @Test
  void rescheduledEvent_createsInAppItem_andEmails() {
    publish(
        new AppointmentRescheduled(
            UUID.randomUUID(),
            beneficiaryId,
            "AG-20260706-0003",
            "CONSULTATION",
            "CARDIOLOGIA",
            UUID.randomUUID(),
            Instant.parse("2026-07-11T12:00:00Z"),
            accountId));

    await(() -> count() == 1 && mail.messages.size() == 1);
    assertThat(eventTypeCode()).isEqualTo("appointment.rescheduled");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("Agendamento reagendado");
  }

  private void publish(Object event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(status -> events.publishEvent(event));
  }

  private long count() {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?", Long.class, accountId);
  }

  private String eventTypeCode() {
    return jdbc.queryForObject(
        "select event_type_code from notification where account_id = ?", String.class, accountId);
  }

  private static void await(BooleanSupplier condition) {
    for (int attempt = 0; attempt < 100; attempt++) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError("appointment notification/e-mail not delivered in time");
  }
}
