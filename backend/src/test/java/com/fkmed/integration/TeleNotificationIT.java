package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.clinicaldocs.ClinicalDocumentIssued;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentType;
import com.fkmed.domain.telemedicine.TeleSessionClosed;
import com.fkmed.domain.telemedicine.TeleTurnReached;
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
 * Cross-spec wiring SPEC-0010 × SPEC-0011 × SPEC-0004 (V20): each telemedicine turn/closure and the
 * clinical-document issuance becomes the corresponding in-app notification for the owning account
 * and dispatches its e-mail AFTER_COMMIT (business, opt-outable). The account/e-mail are resolved
 * from the beneficiary via {@code IdentityAccounts}. Rows/mails are wiped per test for
 * absolute-count isolation on the shared Postgres (both {@code @BeforeEach} and
 * {@code @AfterEach}).
 */
@Import(RecordingMailConfig.class)
class TeleNotificationIT extends AbstractIntegrationTest {

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
  void turnReached_createsInAppItem_andEmailsTheAccountAddress() {
    publish(new TeleTurnReached(UUID.randomUUID(), beneficiaryId, "Dra. Ana", accountId));

    await(() -> count() == 1 && mail.messages.size() == 1);

    assertThat(eventTypeCode()).isEqualTo("tele.turn-reached");
    assertThat(mail.messages.getFirst().to()).isEqualTo("maria@fkmed.local");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("É a sua vez na Telemedicina");
  }

  @Test
  void sessionClosed_createsInAppItem_andEmails() {
    publish(
        new TeleSessionClosed(
            UUID.randomUUID(),
            beneficiaryId,
            "Dra. Ana",
            "CRM-RJ 12345",
            "Repouso e hidratação",
            accountId,
            Instant.parse("2026-07-06T12:30:00Z")));

    await(() -> count() == 1 && mail.messages.size() == 1);

    assertThat(eventTypeCode()).isEqualTo("tele.session-closed");
    assertThat(mail.messages.getFirst().subject())
        .isEqualTo("Atendimento de Telemedicina encerrado");
    // BR4 / SPEC-0010 §Observability: no clinical guidance leaks into the notification body.
    assertThat(bodyOf()).doesNotContain("Repouso e hidratação");
  }

  @Test
  void documentIssued_createsInAppItem_withMinhaSaudeLink_andEmails() {
    publish(
        new ClinicalDocumentIssued(
            UUID.randomUUID(),
            beneficiaryId,
            ClinicalDocumentType.PRESCRIPTION,
            "/api/clinical-documents/x"));

    await(() -> count() == 1 && mail.messages.size() == 1);

    Map<String, Object> row =
        jdbc.queryForMap(
            "select event_type_code, link, body from notification where account_id = ?", accountId);
    assertThat(row.get("event_type_code")).isEqualTo("clinical-document.issued");
    assertThat(row.get("link")).isEqualTo("/minha-saude");
    assertThat(row.get("body").toString()).contains("receituário");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("Novo documento em Minha Saúde");
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

  private String bodyOf() {
    return jdbc.queryForObject(
        "select body from notification where account_id = ?", String.class, accountId);
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
    throw new AssertionError("tele/document notification or e-mail not delivered in time");
  }
}
