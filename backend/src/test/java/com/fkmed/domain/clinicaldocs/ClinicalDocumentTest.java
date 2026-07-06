package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0011: {@link ClinicalDocument#issue} per type (BR6 fields, BR4/DL-0019 validity stamped at
 * issue) and the BR5 expiry read. Only {@link ClinicalDocuments} constructs documents in
 * production; this test exercises the entity's own factory directly (package-private, same package)
 * since it is the unit that owns the invariants.
 */
class ClinicalDocumentTest {

  private static final UUID BENEFICIARY = UUID.randomUUID();
  private static final DocumentOrigin SESSION_ORIGIN = DocumentOrigin.ofSession(UUID.randomUUID());
  private static final Instant ISSUED_AT = Instant.parse("2026-01-01T12:00:00Z");
  private static final LocalDate ISSUED_DATE = LocalDate.of(2026, 1, 1);

  @Test
  void issue_examOrder_stampsValidUntil90Days_andCarriesItemsAndIndication() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.examOrder(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            "Investigação de fadiga",
            List.of(new ExamItemInput("Hemograma Completo", "40304361")));

    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.getId()).isNotNull();
    assertThat(document.getType()).isEqualTo(ClinicalDocumentType.EXAM_ORDER);
    assertThat(document.getBeneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(document.getValidUntil()).isEqualTo(ISSUED_DATE.plusDays(90));
    assertThat(document.getClinicalIndication()).isEqualTo("Investigação de fadiga");
    assertThat(document.getExamItems()).hasSize(1);
    assertThat(document.getExamItems().get(0).getExamName()).isEqualTo("Hemograma Completo");
    assertThat(document.getExamItems().get(0).getTussCode()).isEqualTo("40304361");
    assertThat(document.getOriginSessionId()).isEqualTo(SESSION_ORIGIN.sessionId());
    assertThat(document.getOriginOperatorId()).isNull();
  }

  @Test
  void issue_examOrder_withoutItems_throws() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.examOrder(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            "Indicação",
            List.of());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE));
  }

  @Test
  void issue_referral_stampsValidUntil90Days_andCarriesSpecialtyAndReason() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.referral(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            "CARDIOLOGIA",
            "Palpitações recorrentes");

    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.getValidUntil()).isEqualTo(ISSUED_DATE.plusDays(90));
    assertThat(document.getTargetSpecialtyCode()).isEqualTo("CARDIOLOGIA");
    assertThat(document.getReferralReason()).isEqualTo("Palpitações recorrentes");
  }

  @Test
  void issue_prescription_stampsValidUntil30Days_andCarriesMedications() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            List.of(
                new PrescriptionItemInput(
                    "Amoxicilina 500mg", "1cp 8/8h por 7 dias", "Com alimentos")));

    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.getValidUntil()).isEqualTo(ISSUED_DATE.plusDays(30));
    assertThat(document.getPrescriptionItems()).hasSize(1);
    assertThat(document.getPrescriptionItems().get(0).getMedication())
        .isEqualTo("Amoxicilina 500mg");
  }

  @Test
  void issue_prescription_withoutMedications_throws() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            BENEFICIARY, "Dra. Camila Andrade", "CRM 55214 RJ", SESSION_ORIGIN, List.of());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE));
  }

  @Test
  void issue_sickNote_hasNoValidity_andCarriesPeriodCidAndNotes() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.sickNote(
            BENEFICIARY,
            "Dr. Rafael Nunes",
            "CRM 48310 RJ",
            SESSION_ORIGIN,
            ISSUED_DATE,
            ISSUED_DATE.plusDays(3),
            "J11",
            "Repouso domiciliar");

    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.getValidUntil()).isNull();
    assertThat(document.getSickNotePeriodStart()).isEqualTo(ISSUED_DATE);
    assertThat(document.getSickNotePeriodEnd()).isEqualTo(ISSUED_DATE.plusDays(3));
    assertThat(document.getCid()).isEqualTo("J11");
    assertThat(document.getSickNoteNotes()).isEqualTo("Repouso domiciliar");
  }

  @Test
  void issue_sickNote_withoutCid_throws() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.sickNote(
            BENEFICIARY,
            "Dr. Rafael Nunes",
            "CRM 48310 RJ",
            SESSION_ORIGIN,
            ISSUED_DATE,
            ISSUED_DATE.plusDays(3),
            null,
            null);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE));
  }

  @Test
  void expired_prescriptionBeforeAndAfterBoundary_day30() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            List.of(new PrescriptionItemInput("Dipirona 500mg", "1cp 6/6h", null)));
    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.expired(ISSUED_DATE.plusDays(30))).isFalse();
    assertThat(document.expired(ISSUED_DATE.plusDays(31))).isTrue();
  }

  @Test
  void expired_sickNote_neverExpires() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.sickNote(
            BENEFICIARY,
            "Dr. Rafael Nunes",
            "CRM 48310 RJ",
            SESSION_ORIGIN,
            ISSUED_DATE,
            ISSUED_DATE.plusDays(3),
            "J11",
            null);
    ClinicalDocument document = ClinicalDocument.issue(command, ISSUED_AT, ISSUED_DATE);

    assertThat(document.expired(LocalDate.of(2099, 12, 31))).isFalse();
  }
}
