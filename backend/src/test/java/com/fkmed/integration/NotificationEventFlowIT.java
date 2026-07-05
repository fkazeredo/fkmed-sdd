package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.identity.PasswordChanged;
import com.fkmed.domain.notification.NotificationRequest;
import com.fkmed.domain.notification.Notifications;
import com.fkmed.infra.email.MailMessage;
import java.util.Map;
import java.util.UUID;
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
 * SPEC-0004 §Events/BR6/BR7 over the real schema (Testcontainers): the identity {@link
 * PasswordChanged} event becomes an in-app notification without the module double-sending its
 * e-mail (DL-0008), an e-mail-defaulting type dispatches its e-mail AFTER_COMMIT, and an opt-out
 * suppresses only the e-mail while the in-app item still appears (AC3). Notification rows are wiped
 * in {@code @BeforeEach}/{@code @AfterEach} for absolute-count isolation.
 */
@Import(RecordingMailConfig.class)
class NotificationEventFlowIT extends AbstractIntegrationTest {

  @Autowired private Notifications notifications;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification");
    jdbc.update("delete from notification_preference");
    mail.messages.clear();
  }

  @Test
  void passwordChangedEvent_createsUnreadInAppItem_andModuleSendsNoEmail() {
    UUID account = UUID.randomUUID();
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status ->
                events.publishEvent(
                    new PasswordChanged(
                        account,
                        "maria@fkmed.local",
                        UUID.randomUUID(),
                        PasswordChanged.SELF_CHANGE)));

    awaitNotification(account);
    Map<String, Object> row =
        jdbc.queryForMap("select * from notification where account_id = ?", account);
    assertThat(row.get("event_type_code")).isEqualTo("account.password-changed");
    assertThat(row.get("read_at")).isNull();
    // DL-0008: email_default=false → the module creates only the in-app item; the identity e-mail
    // seam already sends the "se não foi você" notice, so there is no double-send from here.
    assertThat(mail.messages).extracting(MailMessage::subject).doesNotContain("Senha alterada");
  }

  @Test
  void emailDefaultingType_dispatchesEmailAfterCommit_andCreatesInAppItem() {
    UUID account = UUID.randomUUID();
    notifications.notify(
        new NotificationRequest(
            account,
            "reimbursement.paid",
            "Reembolso pago",
            "Seu reembolso foi pago: R$ 120,00.",
            "/reembolso/RE-1",
            "user@fkmed.local"));

    assertThat(count(account)).isEqualTo(1);
    assertThat(mail.messages).hasSize(1);
    assertThat(mail.messages.getFirst().to()).isEqualTo("user@fkmed.local");
    assertThat(mail.messages.getFirst().subject()).isEqualTo("Reembolso pago");
  }

  @Test
  void optOutOfNonMandatoryType_keepsInAppItem_butSendsNoEmail() {
    UUID account = UUID.randomUUID();
    jdbc.update(
        "insert into notification_preference (account_id, event_type_code, email_opt_out)"
            + " values (?, 'reimbursement.paid', true)",
        account);

    notifications.notify(
        new NotificationRequest(
            account, "reimbursement.paid", "Reembolso pago", "corpo", null, "user@fkmed.local"));

    assertThat(count(account)).isEqualTo(1);
    assertThat(mail.messages).isEmpty();
  }

  private long count(UUID account) {
    return jdbc.queryForObject(
        "select count(*) from notification where account_id = ?", Long.class, account);
  }

  private void awaitNotification(UUID account) {
    for (int attempt = 0; attempt < 100; attempt++) {
      if (count(account) > 0) {
        return;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      }
    }
    throw new AssertionError("notification for account " + account + " was not created in time");
  }
}
