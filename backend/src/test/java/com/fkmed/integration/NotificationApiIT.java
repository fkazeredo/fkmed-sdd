package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC-0004 §API Contracts / §Error Behavior: the notification-center endpoints over the real
 * schema (Testcontainers). Scoped to the JWT subject's account (seeded MARIA); notification rows
 * are wiped in {@code @BeforeEach} and {@code @AfterEach} (absolute-count isolation on the shared
 * Postgres, docs/architecture/testing.md). The permanent V10 catalog seed is left untouched.
 */
class NotificationApiIT extends AbstractIntegrationTest {

  private static final UUID MARIA_ACCOUNT = UUID.fromString("d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70");
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID OTHER_ACCOUNT = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final Instant BASE = Instant.parse("2026-07-01T12:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void cleanNotifications() {
    jdbc.update("delete from notification");
    jdbc.update("delete from notification_preference");
  }

  @Test
  void list_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
  }

  @Test
  void list_returnsUnreadCountAndItemsNewestFirst_scopedToTheAccount() throws Exception {
    UUID read =
        insert(MARIA_ACCOUNT, "reimbursement.paid", "Antigo lido", BASE, BASE.plusSeconds(5));
    insert(MARIA_ACCOUNT, "guide.status-changed", "Meio", BASE.plusSeconds(10), null);
    insert(MARIA_ACCOUNT, "appointment.confirmed", "Mais novo", BASE.plusSeconds(20), null);
    // A notification of another account must never leak into MARIA's list.
    insert(OTHER_ACCOUNT, "reimbursement.paid", "De outra conta", BASE.plusSeconds(30), null);

    mockMvc
        .perform(get("/api/notifications").with(mariaJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unread").value(2))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].title").value("Mais novo"))
        .andExpect(jsonPath("$.items[0].type").value("appointment.confirmed"))
        .andExpect(jsonPath("$.items[0].read").value(false))
        .andExpect(jsonPath("$.items[1].title").value("Meio"))
        .andExpect(jsonPath("$.items[2].title").value("Antigo lido"))
        .andExpect(jsonPath("$.items[2].read").value(true))
        .andExpect(jsonPath("$.items[2].id").value(read.toString()));
  }

  @Test
  void list_isPaginated_newestFirst() throws Exception {
    for (int i = 0; i < 25; i++) {
      insert(MARIA_ACCOUNT, "reimbursement.paid", "n" + i, BASE.plusSeconds(i), null);
    }
    mockMvc
        .perform(get("/api/notifications").param("page", "0").param("size", "20").with(mariaJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unread").value(25))
        .andExpect(jsonPath("$.items.length()").value(20))
        .andExpect(jsonPath("$.items[0].title").value("n24"));
    mockMvc
        .perform(get("/api/notifications").param("page", "1").param("size", "20").with(mariaJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(5))
        .andExpect(jsonPath("$.items[0].title").value("n4"));
  }

  @Test
  void markRead_marksItemAndDecrementsUnread() throws Exception {
    UUID id = insert(MARIA_ACCOUNT, "reimbursement.paid", "Não lida", BASE, null);

    mockMvc
        .perform(post("/api/notifications/{id}/read", id).with(mariaJwt()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/notifications").with(mariaJwt()))
        .andExpect(jsonPath("$.unread").value(0))
        .andExpect(jsonPath("$.items[0].read").value(true));
  }

  @Test
  void markRead_unknownId_returns404WithCode() throws Exception {
    mockMvc
        .perform(post("/api/notifications/{id}/read", UUID.randomUUID()).with(mariaJwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("notification.not-found"));
  }

  @Test
  void markRead_foreignId_returns404_andDoesNotMarkIt() throws Exception {
    UUID foreign = insert(OTHER_ACCOUNT, "reimbursement.paid", "De outra conta", BASE, null);

    mockMvc
        .perform(post("/api/notifications/{id}/read", foreign).with(mariaJwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("notification.not-found"));

    Timestamp readAt =
        jdbc.queryForObject(
            "select read_at from notification where id = ?", Timestamp.class, foreign);
    org.assertj.core.api.Assertions.assertThat(readAt).isNull();
  }

  @Test
  void readAll_marksEveryUnreadItemOfTheAccount() throws Exception {
    insert(MARIA_ACCOUNT, "reimbursement.paid", "a", BASE, null);
    insert(MARIA_ACCOUNT, "guide.status-changed", "b", BASE.plusSeconds(1), null);
    UUID otherUnread = insert(OTHER_ACCOUNT, "reimbursement.paid", "c", BASE, null);

    mockMvc
        .perform(post("/api/notifications/read-all").with(mariaJwt()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/notifications").with(mariaJwt()))
        .andExpect(jsonPath("$.unread").value(0));
    // Another account's unread items are untouched.
    Timestamp otherReadAt =
        jdbc.queryForObject(
            "select read_at from notification where id = ?", Timestamp.class, otherUnread);
    org.assertj.core.api.Assertions.assertThat(otherReadAt).isNull();
  }

  @Test
  void preferences_returnsCatalogWithMandatoryFlagsAndDefaults() throws Exception {
    mockMvc
        .perform(get("/api/notifications/preferences").with(mariaJwt()))
        .andExpect(status().isOk())
        // 6 types (V10) + 2 appointment types (V17) + 3 tele/document types (V20, Phase-4 Wave 2).
        .andExpect(jsonPath("$.preferences.length()").value(11))
        .andExpect(
            jsonPath("$.preferences[?(@.type=='account.password-changed')].mandatory").value(true))
        .andExpect(
            jsonPath("$.preferences[?(@.type=='account.password-changed')].emailOptOut")
                .value(false))
        .andExpect(
            jsonPath("$.preferences[?(@.type=='reimbursement.paid')].mandatory").value(false));
  }

  @Test
  void updatePreferences_optOutOfNonMandatoryType_isPersisted() throws Exception {
    mockMvc
        .perform(
            put("/api/notifications/preferences")
                .with(mariaJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"preferences\":[{\"type\":\"reimbursement.paid\",\"emailOptOut\":true}]}"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.preferences[?(@.type=='reimbursement.paid')].emailOptOut").value(true));

    Boolean optOut =
        jdbc.queryForObject(
            "select email_opt_out from notification_preference where account_id = ? and"
                + " event_type_code = 'reimbursement.paid'",
            Boolean.class,
            MARIA_ACCOUNT);
    org.assertj.core.api.Assertions.assertThat(optOut).isTrue();
  }

  @Test
  void updatePreferences_optOutOfMandatoryType_returns422_andPersistsNothing() throws Exception {
    mockMvc
        .perform(
            put("/api/notifications/preferences")
                .with(mariaJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"preferences\":[{\"type\":\"account.password-changed\",\"emailOptOut\":true}]}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("notification.preference-mandatory"));

    Long rows =
        jdbc.queryForObject(
            "select count(*) from notification_preference where account_id = ?",
            Long.class,
            MARIA_ACCOUNT);
    org.assertj.core.api.Assertions.assertThat(rows).isZero();
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor mariaJwt() {
    return jwt().jwt(builder -> builder.subject(MARIA_EMAIL));
  }

  private UUID insert(UUID account, String type, String title, Instant createdAt, Instant readAt) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into notification (id, account_id, event_type_code, title, body, link,"
            + " created_at, read_at) values (?, ?, ?, ?, 'corpo', null, ?, ?)",
        id,
        account,
        type,
        title,
        Timestamp.from(createdAt),
        readAt == null ? null : Timestamp.from(readAt));
    return id;
  }
}
