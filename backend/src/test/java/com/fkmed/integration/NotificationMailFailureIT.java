package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fkmed.domain.notification.NotificationRequest;
import com.fkmed.domain.notification.Notifications;
import com.fkmed.infra.email.MailSender;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * SPEC-0004 BR6: a mail-sender failure during AFTER_COMMIT delivery must NOT roll back or fail the
 * business transaction that raised the notification. With a {@code MailSender} that always throws,
 * an e-mail-defaulting notification is still created and {@code notify} returns normally — the
 * outage is swallowed by the dispatcher and the already-committed in-app row survives.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Import(NotificationMailFailureIT.FailingMailConfig.class)
class NotificationMailFailureIT {

  @ServiceConnection static final PostgreSQLContainer POSTGRES = SharedPostgres.INSTANCE;

  @Autowired private Notifications notifications;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void clean() {
    jdbc.update("delete from notification");
    jdbc.update("delete from notification_preference");
  }

  @Test
  void mailSenderFailure_doesNotRollBackOrFailTheNotification() {
    UUID account = UUID.randomUUID();

    assertThatCode(
            () ->
                notifications.notify(
                    new NotificationRequest(
                        account,
                        "reimbursement.paid",
                        "Reembolso pago",
                        "corpo",
                        null,
                        "user@fkmed.local")))
        .doesNotThrowAnyException();

    Long rows =
        jdbc.queryForObject(
            "select count(*) from notification where account_id = ?", Long.class, account);
    assertThat(rows).isEqualTo(1L);
  }

  @TestConfiguration
  static class FailingMailConfig {
    @Bean
    @Primary
    MailSender failingMailSender() {
      return message -> {
        throw new IllegalStateException("SMTP down");
      };
    }
  }
}
