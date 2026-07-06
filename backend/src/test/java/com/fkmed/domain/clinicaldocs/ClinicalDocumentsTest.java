package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * SPEC-0011 §Business Context/BR8/BR4: the internal issuance facade — the only construction path
 * for a document — stamps {@code valid_until} at issue and publishes {@link ClinicalDocumentIssued}
 * inside the same call.
 */
@ExtendWith(MockitoExtension.class)
class ClinicalDocumentsTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
  private static final UUID BENEFICIARY = UUID.randomUUID();
  private static final DocumentOrigin SESSION_ORIGIN = DocumentOrigin.ofSession(UUID.randomUUID());

  @Mock private ClinicalDocumentRepository documents;
  @Mock private ApplicationEventPublisher events;

  private ClinicalDocuments facade;

  @BeforeEach
  void setUp() {
    facade = new ClinicalDocuments(documents, events, CLOCK);
  }

  @Test
  void issue_prescription_stampsValidUntil30Days_savesAndPublishesEvent() {
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.prescription(
            BENEFICIARY,
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            SESSION_ORIGIN,
            List.of(new PrescriptionItemInput("Amoxicilina 500mg", "1cp 8/8h", null)));

    UUID documentId = facade.issue(command);

    assertThat(documentId).isNotNull();

    ArgumentCaptor<ClinicalDocument> saved = ArgumentCaptor.forClass(ClinicalDocument.class);
    verify(documents).save(saved.capture());
    assertThat(saved.getValue().getId()).isEqualTo(documentId);
    assertThat(saved.getValue().getValidUntil()).isEqualTo(LocalDate.of(2026, 7, 6).plusDays(30));
    assertThat(saved.getValue().getIssuedAt()).isEqualTo(CLOCK.instant());

    ArgumentCaptor<ClinicalDocumentIssued> published =
        ArgumentCaptor.forClass(ClinicalDocumentIssued.class);
    verify(events).publishEvent(published.capture());
    assertThat(published.getValue().documentId()).isEqualTo(documentId);
    assertThat(published.getValue().beneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(published.getValue().type()).isEqualTo(ClinicalDocumentType.PRESCRIPTION);
    assertThat(published.getValue().link()).isEqualTo("/api/clinical-documents/" + documentId);
  }

  @Test
  void issue_sickNote_publishesEventWithNullValidity() {
    when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    IssueClinicalDocumentCommand command =
        IssueClinicalDocumentCommand.sickNote(
            BENEFICIARY,
            "Dr. Rafael Nunes",
            "CRM 48310 RJ",
            SESSION_ORIGIN,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 3),
            "J11",
            null);

    facade.issue(command);

    ArgumentCaptor<ClinicalDocument> saved = ArgumentCaptor.forClass(ClinicalDocument.class);
    verify(documents).save(saved.capture());
    assertThat(saved.getValue().getValidUntil()).isNull();
  }
}
