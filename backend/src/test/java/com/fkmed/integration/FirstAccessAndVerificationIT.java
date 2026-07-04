package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.infra.email.MailMessage;
import com.fkmed.infra.email.MailSender;
import com.jayway.jsonpath.JsonPath;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SPEC-0002 acceptance ITs over the real stack (Testcontainers + MockMvc): the PEDRO first-access +
 * verification happy path (AC1), each refusal (AC2 generic not-found, AC3 already-registered, AC9
 * underage dependent), plus BR4 e-mail uniqueness and BR9 password policy — with the verification
 * link captured from a recording {@link MailSender} and the audit trail (SPEC-0003 BR6) asserted.
 */
@Import(FirstAccessAndVerificationIT.RecordingMailConfig.class)
class FirstAccessAndVerificationIT extends AbstractIntegrationTest {

  private static final String MARIA_ACCOUNT_EMAIL = "maria@fkmed.local";
  private static final String MARIA_BENEFICIARY_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String PLAN_ID = "b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a";
  private static final String PEDRO_BENEFICIARY_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
  private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9\\-_%]+)");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RecordingMailSender mail;

  @BeforeEach
  @org.junit.jupiter.api.AfterEach
  void isolate() {
    // Isolation before AND after each test: this IT creates accounts and (AC9) a dependent, so it
    // must leave the shared DB with only the seeded MARIA account + the V1 beneficiaries —
    // otherwise
    // it leaks into other classes' expectations (e.g. PlanApiIT's family size).
    jdbc.update("delete from email_verification_token");
    jdbc.update("delete from term_acceptance");
    jdbc.update("delete from audit_event");
    jdbc.update("delete from user_account where email <> ?", MARIA_ACCOUNT_EMAIL);
    jdbc.update("delete from beneficiary where card_number not in ('001234567', '001234575')");
    mail.messages.clear();
  }

  @Test
  void ac1_pedroFirstAccess_verifyThenCompleteThenConfirm_activatesTheAccount() throws Exception {
    String registrationToken =
        verify("15350946056", "001234575", "2007-05-20")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(registrationToken, "$.registrationToken");

    mockMvc
        .perform(
            post("/api/auth/first-access/complete")
                .contentType("application/json")
                .content(completeBody(token, "pedro@fkmed.local", "Pedro1234")))
        .andExpect(status().isCreated());

    // Account created, still unverified; account-created audited with PEDRO as target.
    assertThat(accountStatus("pedro@fkmed.local")).isEqualTo("EMAIL_NOT_VERIFIED");
    assertThat(auditCount("identity.account-created", PEDRO_BENEFICIARY_ID)).isEqualTo(1);

    // The verification e-mail carried a link; confirming it activates the account.
    String verificationToken = extractToken(mail.messages.getLast().body());
    mockMvc
        .perform(
            post("/api/auth/verification/confirm")
                .contentType("application/json")
                .content("{\"token\":\"" + verificationToken + "\"}"))
        .andExpect(status().isOk());

    assertThat(accountStatus("pedro@fkmed.local")).isEqualTo("ACTIVE");
    assertThat(auditCount("identity.email-verified", PEDRO_BENEFICIARY_ID)).isEqualTo(1);
    assertThat(termAcceptanceCount("pedro@fkmed.local")).isEqualTo(2);
  }

  @Test
  void ac2_correctCardWrongBirthDate_yieldsTheGenericNotFound() throws Exception {
    verify("15350946056", "001234575", "2000-01-01")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.registration-not-found"));
  }

  @Test
  void ac3_beneficiaryWithAnAccount_isDirectedToLogin_andNoAccountIsCreated() throws Exception {
    long before = accountCount();
    verify("52998224725", "001234567", "1988-03-12")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("auth.account-already-exists"));
    assertThat(accountCount()).isEqualTo(before);
  }

  @Test
  void ac9_underageDependent_isRefusedWithTitularGuidance() throws Exception {
    insertUnderageDependent();
    verify("12345678909", "001234580", "2012-01-01")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.dependent-underage"));
  }

  @Test
  void br4_reusedEmail_isRefused() throws Exception {
    String token =
        JsonPath.read(
            verify("15350946056", "001234575", "2007-05-20")
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.registrationToken");

    mockMvc
        .perform(
            post("/api/auth/first-access/complete")
                .contentType("application/json")
                .content(completeBody(token, MARIA_ACCOUNT_EMAIL, "Pedro1234")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("auth.email-already-used"));
  }

  @Test
  void br9_weakPassword_isRefusedWithThePolicyCode() throws Exception {
    String token =
        JsonPath.read(
            verify("15350946056", "001234575", "2007-05-20")
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.registrationToken");

    mockMvc
        .perform(
            post("/api/auth/first-access/complete")
                .contentType("application/json")
                .content(completeBody(token, "pedro@fkmed.local", "short")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("auth.password-policy-violation"));
  }

  @Test
  void expiredOrUnknownVerificationToken_isGone() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/verification/confirm")
                .contentType("application/json")
                .content("{\"token\":\"an-unknown-token\"}"))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.code").value("auth.verification-link-invalid"));
  }

  private org.springframework.test.web.servlet.ResultActions verify(
      String cpf, String card, String birthDate) throws Exception {
    return mockMvc.perform(
        post("/api/auth/first-access/verify")
            .contentType("application/json")
            .content(
                "{\"cpf\":\"%s\",\"cardNumber\":\"%s\",\"birthDate\":\"%s\"}"
                    .formatted(cpf, card, birthDate)));
  }

  private static String completeBody(String token, String email, String password) {
    return "{\"registrationToken\":\"%s\",\"email\":\"%s\",\"password\":\"%s\","
            .formatted(token, email, password)
        + "\"acceptedTerms\":true,\"acceptedPrivacy\":true}";
  }

  private static String extractToken(String body) {
    Matcher matcher = TOKEN.matcher(body);
    assertThat(matcher.find()).as("verification e-mail must contain a token link").isTrue();
    return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
  }

  private String accountStatus(String email) {
    return jdbc.queryForObject(
        "select status from user_account where email = ?", String.class, email);
  }

  private long accountCount() {
    return jdbc.queryForObject("select count(*) from user_account", Long.class);
  }

  private long auditCount(String eventType, String targetBeneficiaryId) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ? and target_beneficiary_id = ?::uuid",
        Long.class,
        eventType,
        targetBeneficiaryId);
  }

  private long termAcceptanceCount(String email) {
    return jdbc.queryForObject(
        "select count(*) from term_acceptance t join user_account u on u.id = t.account_id"
            + " where u.email = ?",
        Long.class,
        email);
  }

  private void insertUnderageDependent() {
    jdbc.update(
        "insert into beneficiary (id, plan_id, full_name, cpf, cns, card_number, birth_date,"
            + " role, titular_id, active) values (gen_random_uuid(), ?::uuid, 'LUCAS SOUZA LIMA',"
            + " '12345678909', '700000000000003', '001234580', date '2012-01-01', 'DEPENDENT',"
            + " ?::uuid, true)",
        PLAN_ID,
        MARIA_BENEFICIARY_ID);
  }

  /** Captures sent mail so the IT can read the verification link (ADR-0004 seam under test). */
  static final class RecordingMailSender implements MailSender {
    final List<MailMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public void send(MailMessage message) {
      messages.add(message);
    }
  }

  @TestConfiguration
  static class RecordingMailConfig {
    @Bean
    @Primary
    RecordingMailSender recordingMailSender() {
      return new RecordingMailSender();
    }
  }
}
