package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.plan.ContactDataChanged;
import com.fkmed.infra.email.MailMessage;
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
 * Cross-spec wiring SPEC-0006 §Events × SPEC-0004: a contact-e-mail change publishes {@code
 * ContactDataChanged}, and the notification module (AFTER_COMMIT) creates the mandatory {@code
 * account.contact-changed} in-app item for the owning account and e-mails BOTH the old and the new
 * address (security notice). Rows/mails are wiped per test for absolute-count isolation on the
 * shared Postgres.
 */
@Import(RecordingMailConfig.class)
class NotificationContactChangedIT extends AbstractIntegrationTest {

  @Autowired private ApplicationEventPublisher events;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;
  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification");
    mail.messages.clear();
  }

  @Test
  void contactDataChanged_notifiesAccountInApp_andEmailsOldAndNewAddresses() {
    Map<String, Object> maria =
        jdbc.queryForMap(
            "select id, beneficiary_id from user_account where email = ?", "maria@fkmed.local");
    UUID accountId = UUID.fromString(maria.get("id").toString());
    UUID beneficiaryId = UUID.fromString(maria.get("beneficiary_id").toString());

    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status ->
                events.publishEvent(
                    new ContactDataChanged(
                        beneficiaryId, "antigo@fkmed.local", "novo@fkmed.local", Instant.now())));

    await(() -> count(accountId) == 1 && mail.messages.size() == 2);

    Map<String, Object> row =
        jdbc.queryForMap(
            "select event_type_code, read_at from notification where account_id = ?", accountId);
    assertThat(row.get("event_type_code")).isEqualTo("account.contact-changed");
    assertThat(row.get("read_at")).isNull();
    assertThat(mail.messages)
        .extracting(MailMessage::to)
        .containsExactlyInAnyOrder("antigo@fkmed.local", "novo@fkmed.local");
  }

  private long count(UUID account) {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?", Long.class, account);
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
    throw new AssertionError("contact-changed notification/e-mails not delivered in time");
  }
}
