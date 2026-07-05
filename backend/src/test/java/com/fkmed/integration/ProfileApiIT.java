package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fkmed.domain.plan.ContactDataChanged;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * SPEC-0006 profile endpoints over the real schema (Testcontainers): read-only contract data +
 * masked CPF (BR4), partial contact updates with the mandatory-field guard, audit trail and the
 * AFTER_COMMIT {@link ContactDataChanged} event (BR6/BR7), photo upload/serve/remove with
 * magic-byte and size validation (BR2/BR3), family-scope reuse (SPEC-0003 — AC4). MARIA is
 * Flyway-seeded; PEDRO is a disposable fixture. The mutated MARIA/PEDRO contacts and the photo rows
 * are restored/cleaned in {@code @BeforeEach} AND {@code @AfterEach} (Postgres is a suite
 * singleton).
 */
@Import(ProfileApiIT.CaptureConfig.class)
class ProfileApiIT extends AbstractIntegrationTest {

  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ContactDataChangedRecorder recorder;

  @BeforeEach
  void setUp() {
    cleanState();
    PedroAccountFixture.seed(jdbc);
  }

  @AfterEach
  void tearDown() {
    cleanState();
    PedroAccountFixture.remove(jdbc);
  }

  private void cleanState() {
    jdbc.update("delete from beneficiary_photo");
    jdbc.update(
        "delete from audit_event where event_type in"
            + " ('profile.contact-data-changed', 'profile.photo-changed')");
    restoreContacts();
    recorder.events.clear();
  }

  @Test
  void profile_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(get("/api/beneficiaries/{id}/profile", MARIA_ID))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void profile_asMaria_showsContractReadOnlyMaskedAndContacts() throws Exception {
    mockMvc
        .perform(get("/api/beneficiaries/{id}/profile", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("MARIA CLARA SOUZA LIMA"))
        .andExpect(jsonPath("$.cpf").value("*********25"))
        .andExpect(jsonPath("$.birthDate").value("1988-03-12"))
        .andExpect(jsonPath("$.cardNumber").value("001234567"))
        .andExpect(jsonPath("$.contactEmail").value("maria.contato@fkmed.local"))
        .andExpect(jsonPath("$.mobile").value("(21) 99876-5432"))
        .andExpect(jsonPath("$.uf").value("RJ"))
        .andExpect(jsonPath("$.avatarUrl").doesNotExist());
  }

  @Test
  void patchContacts_partialMobile_persistsReopenAndAudits_withoutEmailEvent() throws Exception {
    patchAs(MARIA_EMAIL, MARIA_ID, "{\"mobile\":\"(11) 98888-7777\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mobile").value("(11) 98888-7777"))
        .andExpect(jsonPath("$.contactEmail").value("maria.contato@fkmed.local"));

    // Reopen: the new value persisted and the untouched e-mail is unchanged (AC2).
    mockMvc
        .perform(get("/api/beneficiaries/{id}/profile", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(jsonPath("$.mobile").value("(11) 98888-7777"))
        .andExpect(jsonPath("$.contactEmail").value("maria.contato@fkmed.local"));

    assertThat(auditCount("profile.contact-data-changed")).isEqualTo(1);
    assertThat(recorder.events).isEmpty(); // e-mail unchanged → no ContactDataChanged
  }

  @Test
  void patchContacts_emptyMobile_isRefused422() throws Exception {
    patchAs(MARIA_EMAIL, MARIA_ID, "{\"mobile\":\"\"}")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("profile.mobile-required"));
  }

  @Test
  void patchContacts_invalidUf_isRefused422() throws Exception {
    patchAs(MARIA_EMAIL, MARIA_ID, "{\"uf\":\"ZZ\"}")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("profile.uf-invalid"));
  }

  @Test
  void patchContacts_changingEmail_publishesContactDataChangedAfterCommit() throws Exception {
    patchAs(MARIA_EMAIL, MARIA_ID, "{\"contactEmail\":\"nova@contato.com\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contactEmail").value("nova@contato.com"));

    assertThat(recorder.events).hasSize(1);
    ContactDataChanged event = recorder.events.getFirst();
    assertThat(event.oldEmail()).isEqualTo("maria.contato@fkmed.local");
    assertThat(event.newEmail()).isEqualTo("nova@contato.com");
    assertThat(auditCount("profile.contact-data-changed")).isEqualTo(1);
  }

  @Test
  void patchContacts_asPedro_ofMaria_isDenied404() throws Exception {
    patchAs(PEDRO_EMAIL, MARIA_ID, "{\"mobile\":\"(11) 98888-7777\"}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("context.beneficiary-not-accessible"));
  }

  @Test
  void patchContacts_asPedro_ofHimself_isAllowed() throws Exception {
    patchAs(PEDRO_EMAIL, PEDRO_ID, "{\"mobile\":\"(11) 97777-6666\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mobile").value("(11) 97777-6666"));
  }

  @Test
  void photo_uploadValidPng_isServedAndSetsAvatarUrl_andAudits() throws Exception {
    uploadPhoto(MARIA_EMAIL, MARIA_ID, "image/png", png(64)).andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/beneficiaries/{id}/photo", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"));
    mockMvc
        .perform(get("/api/beneficiaries/{id}/profile", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(jsonPath("$.avatarUrl").value("/api/beneficiaries/" + MARIA_ID + "/photo"));
    assertThat(auditCount("profile.photo-changed")).isEqualTo(1);
  }

  @Test
  void photo_uploadExecutableRenamedPng_isRefused422() throws Exception {
    byte[] pe = {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03};
    uploadPhoto(MARIA_EMAIL, MARIA_ID, "image/png", pe)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("profile.photo-invalid-content"));
  }

  @Test
  void photo_uploadOverFiveMegabytes_isRefused422() throws Exception {
    uploadPhoto(MARIA_EMAIL, MARIA_ID, "image/png", png(5 * 1024 * 1024 + 1))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("profile.photo-too-large"));
  }

  @Test
  void photo_getWhenNoneSet_returns404() throws Exception {
    mockMvc
        .perform(get("/api/beneficiaries/{id}/photo", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound());
  }

  @Test
  void photo_removeReturnsToPlaceholder() throws Exception {
    uploadPhoto(MARIA_EMAIL, MARIA_ID, "image/png", png(64)).andExpect(status().isNoContent());
    mockMvc
        .perform(delete("/api/beneficiaries/{id}/photo", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/beneficiaries/{id}/photo", MARIA_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound());
  }

  @Test
  void photo_titularUploadsForDependent_servesDependentAvatar() throws Exception {
    uploadPhoto(MARIA_EMAIL, PEDRO_ID, "image/png", png(64)).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/beneficiaries/{id}/photo", PEDRO_ID).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"));
  }

  private org.springframework.test.web.servlet.ResultActions patchAs(
      String email, String beneficiaryId, String body) throws Exception {
    return mockMvc.perform(
        patch("/api/beneficiaries/{id}/contacts", beneficiaryId)
            .with(authAs(email))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private org.springframework.test.web.servlet.ResultActions uploadPhoto(
      String email, String beneficiaryId, String contentType, byte[] bytes) throws Exception {
    return mockMvc.perform(
        multipart(HttpMethod.PUT, "/api/beneficiaries/{id}/photo", beneficiaryId)
            .file(new MockMultipartFile("file", "avatar", contentType, bytes))
            .with(authAs(email)));
  }

  private long auditCount(String eventType) {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = ? and target_beneficiary_id is not null",
        Long.class,
        eventType);
  }

  private void restoreContacts() {
    jdbc.update(
        "update beneficiary set contact_email = 'maria.contato@fkmed.local',"
            + " mobile = '(21) 99876-5432', landline = '(21) 2222-1010', cep = '20040002',"
            + " street = 'Avenida Rio Branco', address_number = '156', complement = 'Sala 801',"
            + " neighborhood = 'Centro', city = 'Rio de Janeiro', uf = 'RJ' where id = ?::uuid",
        MARIA_ID);
    jdbc.update(
        "update beneficiary set contact_email = 'pedro.contato@fkmed.local',"
            + " mobile = '(21) 98765-4321', landline = null, cep = '20040002',"
            + " street = 'Avenida Rio Branco', address_number = '156', complement = 'Sala 801',"
            + " neighborhood = 'Centro', city = 'Rio de Janeiro', uf = 'RJ' where id = ?::uuid",
        PEDRO_ID);
  }

  private static byte[] png(int size) {
    byte[] bytes = new byte[size];
    byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    System.arraycopy(magic, 0, bytes, 0, magic.length);
    return bytes;
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }

  /** Captures the AFTER_COMMIT {@link ContactDataChanged} exactly as SPEC-0004's listener will. */
  static class ContactDataChangedRecorder {
    final List<ContactDataChanged> events = new CopyOnWriteArrayList<>();

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ContactDataChanged event) {
      events.add(event);
    }
  }

  @TestConfiguration
  static class CaptureConfig {
    @Bean
    ContactDataChangedRecorder contactDataChangedRecorder() {
      return new ContactDataChangedRecorder();
    }
  }
}
