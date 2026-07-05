package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0006 BR8 legal documents over the real schema (Testcontainers): the current Terms/Privacy
 * versions with per-user acceptance state (interception source), recording an acceptance (audited),
 * the versioned re-acceptance interception when a newer version is published (AC5), and the 409 on
 * accepting an outdated version. MARIA is seeded with acceptance of 1.0 (V12); PEDRO is a
 * disposable fixture with no acceptance. Test-published versions and test-created acceptances are
 * cleaned in {@code @BeforeEach} AND {@code @AfterEach}.
 */
class LegalDocumentApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void setUp() {
    cleanState();
    ensureMariaAcceptedCurrent();
    PedroAccountFixture.seed(jdbc);
  }

  @AfterEach
  void tearDown() {
    cleanState();
    PedroAccountFixture.remove(jdbc);
  }

  private void cleanState() {
    jdbc.update("delete from audit_event where event_type = 'legal.term-accepted'");
    // Keep the seeded 1.0 acceptances/documents; drop anything a test published or accepted.
    jdbc.update("delete from term_acceptance where version <> '1.0'");
    jdbc.update("delete from legal_document where version <> '1.0'");
  }

  /**
   * MARIA accepted the current 1.0 documents at first access (V12 seed). A sibling IT may wipe
   * {@code term_acceptance} on the shared Postgres, so restore it idempotently here rather than
   * depending on the seed surviving the suite (isolation — docs/architecture/testing.md).
   */
  private void ensureMariaAcceptedCurrent() {
    ensureAccepted("TERMS_OF_USE");
    ensureAccepted("PRIVACY_POLICY");
  }

  private void ensureAccepted(String documentType) {
    jdbc.update(
        "insert into term_acceptance (id, account_id, document_type, version, accepted_at)"
            + " values (?::uuid, ?::uuid, ?, '1.0', now())"
            + " on conflict (account_id, document_type, version) do nothing",
        UUID.randomUUID().toString(),
        MARIA_ACCOUNT_ID,
        documentType);
  }

  @Test
  void current_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/legal-documents/current")).andExpect(status().isUnauthorized());
  }

  @Test
  void current_asMaria_showsSeededVersionsAlreadyAccepted() throws Exception {
    mockMvc
        .perform(get("/api/legal-documents/current").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.terms.version").value("1.0"))
        .andExpect(jsonPath("$.terms.acceptedByMe").value(true))
        .andExpect(jsonPath("$.privacy.version").value("1.0"))
        .andExpect(jsonPath("$.privacy.acceptedByMe").value(true));
  }

  @Test
  void current_asPedro_showsNotYetAccepted() throws Exception {
    mockMvc
        .perform(get("/api/legal-documents/current").with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.terms.acceptedByMe").value(false))
        .andExpect(jsonPath("$.privacy.acceptedByMe").value(false));
  }

  @Test
  void document_returnsCurrentBody() throws Exception {
    mockMvc
        .perform(get("/api/legal-documents/{type}", "TERMS").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("TERMS"))
        .andExpect(jsonPath("$.version").value("1.0"))
        .andExpect(jsonPath("$.body").isNotEmpty());
  }

  @Test
  void accept_recordsAcceptance_andAudits() throws Exception {
    mockMvc.perform(accept("TERMS", "1.0", PEDRO_EMAIL)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/legal-documents/current").with(authAs(PEDRO_EMAIL)))
        .andExpect(jsonPath("$.terms.acceptedByMe").value(true));
    assertThat(auditCount()).isEqualTo(1);
  }

  @Test
  void newVersionPublished_intercepts_andOutdatedAcceptIsRejected409() throws Exception {
    publishTermsVersion("2.0");

    // AC5: the newer version is current and not yet accepted → the frontend intercepts.
    mockMvc
        .perform(get("/api/legal-documents/current").with(authAs(MARIA_EMAIL)))
        .andExpect(jsonPath("$.terms.version").value("2.0"))
        .andExpect(jsonPath("$.terms.acceptedByMe").value(false));

    // Accepting the stale 1.0 is rejected.
    mockMvc
        .perform(accept("TERMS", "1.0", MARIA_EMAIL))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("legal.version-outdated"));

    // Accepting the current 2.0 clears the interception.
    mockMvc.perform(accept("TERMS", "2.0", MARIA_EMAIL)).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/legal-documents/current").with(authAs(MARIA_EMAIL)))
        .andExpect(jsonPath("$.terms.acceptedByMe").value(true));
  }

  private org.springframework.test.web.servlet.RequestBuilder accept(
      String type, String version, String email) {
    return post("/api/legal-documents/{type}/accept", type)
        .with(authAs(email))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"version\":\"" + version + "\"}");
  }

  private void publishTermsVersion(String version) {
    jdbc.update(
        "insert into legal_document (id, type, version, published_at, body)"
            + " values (?::uuid, 'TERMS', ?, now(), 'Termos de Uso — versão de teste.')",
        UUID.randomUUID().toString(),
        version);
  }

  private long auditCount() {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = 'legal.term-accepted'", Long.class);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
