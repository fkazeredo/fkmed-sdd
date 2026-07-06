package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentDetail;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentListResponse;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentService;
import com.fkmed.domain.clinicaldocs.ClinicalDocumentType;
import com.fkmed.domain.clinicaldocs.ClinicalDocuments;
import com.fkmed.domain.clinicaldocs.DocumentOrigin;
import com.fkmed.domain.clinicaldocs.DocumentPeriod;
import com.fkmed.domain.clinicaldocs.IssueClinicalDocumentCommand;
import com.fkmed.domain.clinicaldocs.PrescriptionItemInput;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0011 §Business Context/BR3/BR8: the internal issuance facade is the seam SPEC-0010's tele
 * closure and SPEC-0018's operator sim will call in Wave 2 (out of this slice's scope — the
 * telemedicine module does not exist yet). This test drives the facade itself, over real Postgres,
 * confirming an issued document is immediately visible through the read service in the same
 * transaction-committed state (BR3's "MUST appear immediately after the session closes" — proven
 * here at the facade boundary, without touching the tele module).
 */
class ClinicalDocumentIssuanceIT extends AbstractIntegrationTest {

  private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final String MARIA_CARD = "001234567";
  private static final String MARIA_EMAIL = "maria@fkmed.local";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");

  @Autowired private ClinicalDocuments clinicalDocuments;
  @Autowired private ClinicalDocumentService clinicalDocumentService;
  @Autowired private JdbcTemplate jdbc;

  private UUID issuedDocumentId;

  @AfterEach
  void cleanUp() {
    if (issuedDocumentId != null) {
      jdbc.update("delete from prescription_item where document_id = ?::uuid", issuedDocumentId);
      jdbc.update("delete from clinical_document where id = ?::uuid", issuedDocumentId);
    }
  }

  @Test
  void issue_isImmediatelyVisibleThroughListAndDetail_br3() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            MARIA_ID,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            DocumentOrigin.ofSession(UUID.randomUUID()),
            List.of(new PrescriptionItemInput("Ibuprofeno 600mg", "1cp 12/12h por 5 dias", null)));

    issuedDocumentId = clinicalDocuments.issue(command);
    assertThat(issuedDocumentId).isNotNull();

    LocalDate today = LocalDate.now(CLINIC_ZONE);
    ClinicalDocumentListResponse list =
        clinicalDocumentService.list(
            MARIA_CARD,
            MARIA_EMAIL,
            ClinicalDocumentType.PRESCRIPTION,
            MARIA_ID,
            new DocumentPeriod(today.minusDays(1), today),
            AuditContext.none());
    assertThat(list.items()).anyMatch(item -> item.id().equals(issuedDocumentId));

    ClinicalDocumentDetail detail =
        clinicalDocumentService.detail(
            MARIA_CARD, MARIA_EMAIL, issuedDocumentId, AuditContext.none());
    assertThat(detail.medications()).hasSize(1);
    assertThat(detail.medications().get(0).medication()).isEqualTo("Ibuprofeno 600mg");
    assertThat(detail.validUntil()).isEqualTo(today.plusDays(30));
    assertThat(detail.expired()).isFalse();
  }
}
