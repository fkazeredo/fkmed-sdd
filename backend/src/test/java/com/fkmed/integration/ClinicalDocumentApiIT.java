package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0011 API contract (all endpoints + error codes) over MockMvc and Testcontainers Postgres:
 * combined filters (category/beneficiary/period — BR2), the validity badge boundary (BR4/BR5/AC2),
 * type-specific detail (BR6, including the sick-note CID — DL-0020), PDF download (BR7), family
 * scope + the BR9 dependent-access audit (AC3) and the BR9 404 that never reveals existence. The
 * caller's card is resolved server-side from the JWT subject (ADR-0009); dedicated fixtures are
 * used (not the V18 seed) so date-boundary assertions are precise regardless of when the suite
 * runs, and cleaned in {@code @BeforeEach} (Postgres is a shared singleton — testing.md isolation).
 */
class ClinicalDocumentApiIT extends AbstractIntegrationTest {

  private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String MARIA_ACCOUNT_ID = "d4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70";
  private static final String PEDRO_EMAIL = PedroAccountFixture.PEDRO_EMAIL;
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  private static final String ORIGIN_SESSION = "aa100000-0000-4000-8000-000000000001";

  private static final String RX_RECENT = "ff000000-0000-4000-8000-000000000001";
  private static final String RX_OLD_EXPIRED = "ff000000-0000-4000-8000-000000000002";
  private static final String EXAM_ORDER = "ff000000-0000-4000-8000-000000000003";
  private static final String REFERRAL = "ff000000-0000-4000-8000-000000000004";
  private static final String SICK_NOTE = "ff000000-0000-4000-8000-000000000005";
  private static final String RX_PEDRO = "ff000000-0000-4000-8000-000000000006";
  private static final String EXAM_OLD_100D = "ff000000-0000-4000-8000-000000000007";

  private static final List<String> ALL_IDS =
      List.of(RX_RECENT, RX_OLD_EXPIRED, EXAM_ORDER, REFERRAL, SICK_NOTE, RX_PEDRO, EXAM_OLD_100D);

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  private LocalDate today;

  @BeforeEach
  void setUp() {
    clean();
    PedroAccountFixture.seed(jdbc);
    today = LocalDate.now(CLINIC_ZONE);

    // Offsets are deliberately disjoint from the V18 seed's own MARIA/PEDRO offsets
    // ({1,2,3,5,7,10,40,95,100}) — this dedicated fixture set must never collide on the same
    // calendar day as a seeded document for the same beneficiary (testing.md: absolute-count /
    // ordering assertions on a shared table are fragile unless truly isolated).
    insertPrescription(RX_RECENT, MARIA_ID, 6, 30);
    insertPrescriptionItem(
        RX_RECENT, 0, "Amoxicilina 500mg", "1cp 8/8h por 7 dias", "Com alimentos");

    insertPrescription(RX_OLD_EXPIRED, MARIA_ID, 45, 30);
    insertPrescriptionItem(RX_OLD_EXPIRED, 0, "Loratadina 10mg", "1cp ao dia", null);

    insertExamOrder(EXAM_ORDER, MARIA_ID, 14, 90, "Investigação de fadiga");
    insertExamItem(EXAM_ORDER, 0, "Hemograma Completo", "40304361");
    insertExamItem(EXAM_ORDER, 1, "Raio-X de Tórax", "41001114");

    insertReferral(REFERRAL, MARIA_ID, 9, 90, "CARDIOLOGIA", "Palpitações recorrentes");

    insertSickNote(SICK_NOTE, MARIA_ID, 13, "J11", "Repouso domiciliar recomendado");

    insertPrescription(RX_PEDRO, PEDRO_ID, 4, 30);
    insertPrescriptionItem(RX_PEDRO, 0, "Paracetamol 750mg", "1cp 6/6h se febre", null);

    insertExamOrder(EXAM_OLD_100D, PEDRO_ID, 130, 90, "Check-up de rotina");
    insertExamItem(EXAM_OLD_100D, 0, "Hemograma Completo", "40304361");
  }

  @AfterEach
  void tearDown() {
    PedroAccountFixture.remove(jdbc);
  }

  // --- list ---

  @Test
  void list_withoutAuthentication_returns401() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents").param("period", "P365D"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_defaultsToAll_aggregatesAcrossAccessibleBeneficiaries_mostRecentFirst()
      throws Exception {
    String body =
        mockMvc
            .perform(
                get("/api/clinical-documents").param("period", "P365D").with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").exists())
            .andExpect(jsonPath("$.items[?(@.id=='" + RX_PEDRO + "')]").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Ordering check by relative index (robust against any other seeded row interspersed in the
    // shared table — RX_PEDRO (4 days ago) MUST sort before RX_RECENT (6 days ago) regardless of
    // what else appears between/around them).
    List<String> ids = com.jayway.jsonpath.JsonPath.read(body, "$.items[*].id");
    assertThat(ids.indexOf(RX_PEDRO)).isGreaterThanOrEqualTo(0);
    assertThat(ids.indexOf(RX_RECENT)).isGreaterThan(ids.indexOf(RX_PEDRO));
  }

  @Test
  void list_categoryFilter_returnsOnlyThatType() throws Exception {
    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("category", "PRESCRIPTION")
                .param("period", "P365D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.type=='EXAM_ORDER')]").doesNotExist())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").exists())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_OLD_EXPIRED + "')]").exists());
  }

  @Test
  void list_periodP30D_excludesOlderDocuments_ac6() throws Exception {
    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("category", "PRESCRIPTION")
                .param("period", "P30D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").exists())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_OLD_EXPIRED + "')]").doesNotExist());
  }

  @Test
  void list_customPeriod_appliesFromTo() throws Exception {
    // A single exact calendar day matching only RX_RECENT (6 days ago) — disjoint from every V18
    // seed offset, so the exact count is safe to assert (testing.md: absolute-count assertions on
    // a shared table need real isolation, not just a wide window).
    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("period", "custom")
                .param("from", today.minusDays(6).toString())
                .param("to", today.minusDays(6).toString())
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").exists())
        .andExpect(jsonPath("$.items.length()").value(1));
  }

  @Test
  void list_unknownPeriodCode_returns400() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents").param("period", "bogus").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void list_customPeriodMissingBounds_returns400() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents").param("period", "custom").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void list_expiredPrescription_showsExpiredBadge_ac2() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents").param("period", "P365D").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_OLD_EXPIRED + "')].expired").value(true))
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')].expired").value(false));
  }

  @Test
  void list_filteredToSelf_recordsNoAudit() throws Exception {
    long before = countAuditEvents();

    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("beneficiaryId", MARIA_ID)
                .param("period", "P365D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk());

    assertThat(countAuditEvents()).isEqualTo(before);
  }

  @Test
  void list_filteredToDependent_returnsOnlyDependentDocs_andRecordsAudit_ac3() throws Exception {
    long before = countAuditEvents();

    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("beneficiaryId", PEDRO_ID)
                .param("period", "P365D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_PEDRO + "')]").exists())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").doesNotExist());

    assertThat(countAuditEvents()).isEqualTo(before + 1);
    var row =
        jdbc.queryForMap(
            "select * from audit_event where event_type = 'clinicaldocs.dependent-viewed'"
                + " order by occurred_at desc limit 1");
    assertThat(row.get("author_account_id").toString()).isEqualTo(MARIA_ACCOUNT_ID);
    assertThat(row.get("target_beneficiary_id").toString()).isEqualTo(PEDRO_ID);
  }

  @Test
  void list_filteredToOutOfScopeBeneficiary_returnsEmpty() throws Exception {
    mockMvc
        .perform(
            get("/api/clinical-documents")
                .param("beneficiaryId", UUID.randomUUID().toString())
                .param("period", "P365D")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void list_asPedro_neverSeesMariasDocuments() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents").param("period", "P365D").with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_PEDRO + "')]").exists())
        .andExpect(jsonPath("$.items[?(@.id=='" + RX_RECENT + "')]").doesNotExist());
  }

  // --- detail ---

  @Test
  void detail_examOrder_returnsItemsTussAndIndication_ac5() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}", EXAM_ORDER).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EXAM_ORDER"))
        .andExpect(jsonPath("$.clinicalIndication").value("Investigação de fadiga"))
        .andExpect(jsonPath("$.examItems.length()").value(2))
        .andExpect(jsonPath("$.examItems[0].examName").value("Hemograma Completo"))
        .andExpect(jsonPath("$.examItems[0].tussCode").value("40304361"));
  }

  @Test
  void detail_referral_returnsTargetSpecialtyAndReason_ac4() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}", REFERRAL).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("REFERRAL"))
        .andExpect(jsonPath("$.targetSpecialty").value("CARDIOLOGIA"))
        .andExpect(jsonPath("$.reason").value("Palpitações recorrentes"));
  }

  @Test
  void detail_prescription_returnsMedications() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}", RX_RECENT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.medications.length()").value(1))
        .andExpect(jsonPath("$.medications[0].medication").value("Amoxicilina 500mg"));
  }

  @Test
  void detail_sickNote_returnsPeriodCidAndNotes_dl0020() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}", SICK_NOTE).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("SICK_NOTE"))
        .andExpect(jsonPath("$.cid").value("J11"))
        .andExpect(jsonPath("$.validUntil").doesNotExist())
        .andExpect(jsonPath("$.expired").value(false));
  }

  @Test
  void detail_ofDependentDocument_recordsAudit() throws Exception {
    long before = countAuditEvents();

    mockMvc
        .perform(get("/api/clinical-documents/{id}", RX_PEDRO).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk());

    assertThat(countAuditEvents()).isEqualTo(before + 1);
  }

  @Test
  void detail_ofOutOfScopeDocument_returns404_withoutRevealingExistence() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}", RX_RECENT).with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("document.not-found"));
  }

  @Test
  void detail_ofUnknownId_returns404() throws Exception {
    mockMvc
        .perform(
            get("/api/clinical-documents/{id}", "00000000-0000-0000-0000-000000000000")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("document.not-found"));
  }

  // --- pdf ---

  @Test
  void pdf_examOrder_returnsAPdfDocument_downloadable() throws Exception {
    byte[] pdf =
        mockMvc
            .perform(get("/api/clinical-documents/{id}/pdf", EXAM_ORDER).with(authAs(MARIA_EMAIL)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(
                header().string("Content-Disposition", "attachment; filename=\"documento.pdf\""))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }

  @Test
  void pdf_ofExpiredDocument_isStillDownloadable_br5() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}/pdf", RX_OLD_EXPIRED).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
  }

  @Test
  void pdf_ofOutOfScopeDocument_returns404() throws Exception {
    mockMvc
        .perform(get("/api/clinical-documents/{id}/pdf", RX_RECENT).with(authAs(PEDRO_EMAIL)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("document.not-found"));
  }

  // --- immutability: no write path exists (BR8) ---

  @Test
  void noUpdatePath_postToList_returns405() throws Exception {
    mockMvc
        .perform(post("/api/clinical-documents").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void noUpdatePath_putToDetail_returns405() throws Exception {
    mockMvc
        .perform(put("/api/clinical-documents/{id}", RX_RECENT).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isMethodNotAllowed());
  }

  // --- helpers ---

  private void insertPrescription(String id, String beneficiaryId, int daysAgo, int validityDays) {
    LocalDate issuedDate = today.minusDays(daysAgo);
    jdbc.update(
        "insert into clinical_document"
            + " (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,"
            + " origin_session_id)"
            + " values (?::uuid, 'PRESCRIPTION', ?::uuid, 'Dra. Camila Andrade', 'CRM 55214 RJ',"
            + " ?, ?, ?::uuid)",
        id,
        beneficiaryId,
        instantAt(issuedDate),
        issuedDate.plusDays(validityDays),
        ORIGIN_SESSION);
  }

  private void insertPrescriptionItem(
      String documentId, int order, String medication, String posology, String guidance) {
    jdbc.update(
        "insert into prescription_item (document_id, item_order, medication, posology, guidance)"
            + " values (?::uuid, ?, ?, ?, ?)",
        documentId,
        order,
        medication,
        posology,
        guidance);
  }

  private void insertExamOrder(
      String id, String beneficiaryId, int daysAgo, int validityDays, String clinicalIndication) {
    LocalDate issuedDate = today.minusDays(daysAgo);
    jdbc.update(
        "insert into clinical_document"
            + " (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,"
            + " origin_session_id, clinical_indication)"
            + " values (?::uuid, 'EXAM_ORDER', ?::uuid, 'Dra. Camila Andrade', 'CRM 55214 RJ',"
            + " ?, ?, ?::uuid, ?)",
        id,
        beneficiaryId,
        instantAt(issuedDate),
        issuedDate.plusDays(validityDays),
        ORIGIN_SESSION,
        clinicalIndication);
  }

  private void insertExamItem(String documentId, int order, String examName, String tussCode) {
    jdbc.update(
        "insert into exam_order_item (document_id, item_order, exam_name, tuss_code)"
            + " values (?::uuid, ?, ?, ?)",
        documentId,
        order,
        examName,
        tussCode);
  }

  private void insertReferral(
      String id,
      String beneficiaryId,
      int daysAgo,
      int validityDays,
      String targetSpecialtyCode,
      String reason) {
    LocalDate issuedDate = today.minusDays(daysAgo);
    jdbc.update(
        "insert into clinical_document"
            + " (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,"
            + " origin_session_id, target_specialty_code, referral_reason)"
            + " values (?::uuid, 'REFERRAL', ?::uuid, 'Dra. Camila Andrade', 'CRM 55214 RJ',"
            + " ?, ?, ?::uuid, ?, ?)",
        id,
        beneficiaryId,
        instantAt(issuedDate),
        issuedDate.plusDays(validityDays),
        ORIGIN_SESSION,
        targetSpecialtyCode,
        reason);
  }

  private void insertSickNote(
      String id, String beneficiaryId, int daysAgo, String cid, String notes) {
    LocalDate issuedDate = today.minusDays(daysAgo);
    jdbc.update(
        "insert into clinical_document"
            + " (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,"
            + " origin_session_id, sick_note_period_start, sick_note_period_end, cid,"
            + " sick_note_notes)"
            + " values (?::uuid, 'SICK_NOTE', ?::uuid, 'Dr. Rafael Nunes', 'CRM 48310 RJ',"
            + " ?, null, ?::uuid, ?, ?, ?, ?)",
        id,
        beneficiaryId,
        instantAt(issuedDate),
        ORIGIN_SESSION,
        issuedDate,
        issuedDate.plusDays(3),
        cid,
        notes);
  }

  private static Timestamp instantAt(LocalDate date) {
    Instant instant = date.atTime(12, 0).atZone(CLINIC_ZONE).toInstant();
    return Timestamp.from(instant);
  }

  private long countAuditEvents() {
    return jdbc.queryForObject(
        "select count(*) from audit_event where event_type = 'clinicaldocs.dependent-viewed'",
        Long.class);
  }

  private void clean() {
    jdbc.update("delete from audit_event where event_type = 'clinicaldocs.dependent-viewed'");
    jdbc.update(
        "delete from prescription_item where document_id in (?::uuid, ?::uuid, ?::uuid, ?::uuid,"
            + " ?::uuid, ?::uuid, ?::uuid)",
        (Object[]) ALL_IDS.toArray());
    jdbc.update(
        "delete from exam_order_item where document_id in (?::uuid, ?::uuid, ?::uuid, ?::uuid,"
            + " ?::uuid, ?::uuid, ?::uuid)",
        (Object[]) ALL_IDS.toArray());
    jdbc.update(
        "delete from clinical_document where id in (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?::uuid,"
            + " ?::uuid, ?::uuid)",
        (Object[]) ALL_IDS.toArray());
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
