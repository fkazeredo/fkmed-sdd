package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC-0002 slice 1.2 password endpoints over the real stack (Testcontainers + MockMvc): recovery
 * neutrality (BR7/AC8, byte-identical responses), the reset single-use link (BR10/AC5), reset
 * validation, and the authenticated change (BR11) — wrong current password, new-equals-current, and
 * the happy path — with the audit trail (BR14/AC10) and the "não foi você" notice asserted.
 */
@Import(RecordingMailConfig.class)
class AccountSecurityIT extends AbstractIntegrationTest {

  private static final Pattern RESET_TOKEN = Pattern.compile("token=([A-Za-z0-9\\-_%]+)");
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String ACCOUNT_EMAIL = "security-it@fkmed.local";
  private static final String ACCOUNT_PASSWORD = "Senha1234";
  private static final String PEDRO_BENEFICIARY_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  private String seededHash;

  @BeforeEach
  void setUp() {
    clean();
    jdbc.update(
        "insert into user_account (id, beneficiary_id, email, password_hash, status,"
            + " failed_attempts, created_at) values (gen_random_uuid(), ?::uuid, ?,"
            + " '{bcrypt}' || crypt(?, gen_salt('bf', 10)), 'ACTIVE', 0, now())",
        PEDRO_BENEFICIARY_ID,
        ACCOUNT_EMAIL,
        ACCOUNT_PASSWORD);
    seededHash = passwordHash(ACCOUNT_EMAIL);
  }

  @AfterEach
  void tearDown() {
    clean();
  }

  private void clean() {
    jdbc.update("delete from audit_event");
    jdbc.update("delete from password_reset_token");
    jdbc.update("delete from email_verification_token");
    jdbc.update("delete from term_acceptance");
    jdbc.update("delete from user_account where email <> ?", MARIA_EMAIL);
    mail.messages.clear();
  }

  @Test
  void ac8_recoveryRequest_existingAndNonexistent_yieldIdenticalNeutralResponses()
      throws Exception {
    String existing =
        mockMvc
            .perform(recoveryRequest(ACCOUNT_EMAIL))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String nonexistent =
        mockMvc
            .perform(recoveryRequest("nobody@fkmed.local"))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Assertions.assertThat(nonexistent).isEqualTo(existing);
    // Only the real account produced a token, a recovery-requested audit and a reset e-mail.
    Assertions.assertThat(resetTokenCount()).isEqualTo(1);
    Assertions.assertThat(auditCount("identity.password-recovery-requested")).isEqualTo(1);
    Assertions.assertThat(mail.messages).hasSize(1);
  }

  @Test
  void recovery_reset_setsANewPassword_marksTheTokenUsed_auditsAndNotifies() throws Exception {
    mockMvc.perform(recoveryRequest(ACCOUNT_EMAIL)).andExpect(status().isAccepted());
    String token = capturedResetToken();

    mockMvc.perform(resetRequest(token, "BrandNew123")).andExpect(status().isOk());

    Assertions.assertThat(passwordHash(ACCOUNT_EMAIL)).isNotEqualTo(seededHash);
    Assertions.assertThat(usedTokenCount()).isEqualTo(1);
    Assertions.assertThat(auditFlowCount("identity.password-changed", "recovery-reset"))
        .isEqualTo(1);
    // BR10: the change notice ("não foi você? contate os canais") is delivered too.
    Assertions.assertThat(mail.messages).hasSize(2);
  }

  @Test
  void ac5_reusedResetLink_isRejectedAsInvalid() throws Exception {
    mockMvc.perform(recoveryRequest(ACCOUNT_EMAIL)).andExpect(status().isAccepted());
    String token = capturedResetToken();

    mockMvc.perform(resetRequest(token, "BrandNew123")).andExpect(status().isOk());
    mockMvc
        .perform(resetRequest(token, "AnotherOne123"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.code").value("auth.reset-link-invalid"));
  }

  @Test
  void unknownResetToken_isGone() throws Exception {
    mockMvc
        .perform(resetRequest("a-token-that-never-existed", "BrandNew123"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.code").value("auth.reset-link-invalid"));
  }

  @Test
  void reset_withAPolicyViolatingPassword_isRefused() throws Exception {
    mockMvc.perform(recoveryRequest(ACCOUNT_EMAIL)).andExpect(status().isAccepted());
    String token = capturedResetToken();

    mockMvc
        .perform(resetRequest(token, "short"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.password-policy-violation"));
  }

  @Test
  void change_withWrongCurrentPassword_is422() throws Exception {
    mockMvc
        .perform(changeRequest("not-my-password", "BrandNew123"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.current-password-incorrect"));
  }

  @Test
  void change_whenNewEqualsCurrent_isAPolicyViolation() throws Exception {
    mockMvc
        .perform(changeRequest(ACCOUNT_PASSWORD, ACCOUNT_PASSWORD))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.password-policy-violation"));
  }

  @Test
  void change_valid_changesThePassword_auditsSelfChange_andNotifies() throws Exception {
    mockMvc.perform(changeRequest(ACCOUNT_PASSWORD, "Changed1234")).andExpect(status().isOk());

    Assertions.assertThat(passwordHash(ACCOUNT_EMAIL)).isNotEqualTo(seededHash);
    Assertions.assertThat(auditFlowCount("identity.password-changed", "self-change")).isEqualTo(1);
    Assertions.assertThat(mail.messages).hasSize(1);
  }

  @Test
  void change_unauthenticated_is401() throws Exception {
    mockMvc
        .perform(
            put("/api/auth/password")
                .contentType("application/json")
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"BrandNew123\"}"))
        .andExpect(status().isUnauthorized());
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      recoveryRequest(String email) {
    return post("/api/auth/recovery/request")
        .contentType("application/json")
        .content("{\"email\":\"" + email + "\"}");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      resetRequest(String token, String newPassword) {
    return post("/api/auth/recovery/reset")
        .contentType("application/json")
        .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}");
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      changeRequest(String currentPassword, String newPassword) {
    return put("/api/auth/password")
        .with(jwt().jwt(builder -> builder.subject(ACCOUNT_EMAIL)))
        .contentType("application/json")
        .content(
            "{\"currentPassword\":\""
                + currentPassword
                + "\",\"newPassword\":\""
                + newPassword
                + "\"}");
  }

  private String capturedResetToken() {
    Assertions.assertThat(mail.messages).as("a reset e-mail must have been sent").isNotEmpty();
    Matcher matcher = RESET_TOKEN.matcher(mail.messages.getLast().body());
    Assertions.assertThat(matcher.find()).as("the reset e-mail must carry a token link").isTrue();
    return java.net.URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
  }

  private String passwordHash(String email) {
    return jdbc.queryForObject(
        "select password_hash from user_account where email = ?", String.class, email);
  }

  private long resetTokenCount() {
    return jdbc.queryForObject("select count(*) from password_reset_token", Long.class);
  }

  private long usedTokenCount() {
    return jdbc.queryForObject(
        "select count(*) from password_reset_token where used_at is not null", Long.class);
  }

  private long auditCount(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ?", Long.class, eventType);
  }

  private long auditFlowCount(String eventType, String flow) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ? and details->>'flow' = ?",
        Long.class,
        eventType,
        flow);
  }
}
