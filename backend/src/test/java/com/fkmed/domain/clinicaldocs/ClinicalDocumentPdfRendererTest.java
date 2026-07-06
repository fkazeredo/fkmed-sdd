package com.fkmed.domain.clinicaldocs;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0011 BR7/AC5: the PDF must contain issuer, CRM, date, validity and the type-specific body
 * (exam list with TUSS codes; the sick-note CID — DL-0020) as extractable text.
 */
class ClinicalDocumentPdfRendererTest {

  @Test
  void render_producesAValidPdfDocument() {
    byte[] pdf = ClinicalDocumentPdfRenderer.render(examOrderDetail());

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }

  @Test
  void render_examOrder_containsAc5Fields_asExtractableText() throws Exception {
    String text = extractText(ClinicalDocumentPdfRenderer.render(examOrderDetail()));

    assertThat(text).contains("Dra. Camila Andrade");
    assertThat(text).contains("CRM 55214 RJ");
    assertThat(text).contains("04/07/2026");
    assertThat(text).contains("02/10/2026");
    assertThat(text).contains("Hemograma Completo");
    assertThat(text).contains("40304361");
    assertThat(text).contains("Investigação de fadiga");
  }

  @Test
  void render_sickNote_containsCid_asExtractableText() throws Exception {
    ClinicalDocumentDetail detail =
        new ClinicalDocumentDetail(
            UUID.randomUUID(),
            ClinicalDocumentType.SICK_NOTE,
            LocalDate.of(2026, 7, 4),
            "Dr. Rafael Nunes",
            "CRM 48310 RJ",
            "MARIA",
            null,
            false,
            null,
            List.of(),
            null,
            null,
            null,
            List.of(),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 5),
            "J11",
            "Repouso domiciliar");

    String text = extractText(ClinicalDocumentPdfRenderer.render(detail));

    assertThat(text).contains("J11");
    assertThat(text).contains("Sem validade");
    assertThat(text).contains("Repouso domiciliar");
  }

  @Test
  void render_expiredDocument_showsExpiredLabel() throws Exception {
    ClinicalDocumentDetail detail =
        new ClinicalDocumentDetail(
            UUID.randomUUID(),
            ClinicalDocumentType.PRESCRIPTION,
            LocalDate.of(2026, 1, 1),
            "Dra. Camila Andrade",
            "CRM 55214 RJ",
            "MARIA",
            LocalDate.of(2026, 1, 31),
            true,
            null,
            List.of(),
            null,
            null,
            null,
            List.of(
                new ClinicalDocumentDetail.PrescriptionItemView(
                    "Dipirona 500mg", "1cp 6/6h", null)),
            null,
            null,
            null,
            null);

    String text = extractText(ClinicalDocumentPdfRenderer.render(detail));

    assertThat(text).contains("Expirado em");
  }

  private static ClinicalDocumentDetail examOrderDetail() {
    return new ClinicalDocumentDetail(
        UUID.randomUUID(),
        ClinicalDocumentType.EXAM_ORDER,
        LocalDate.of(2026, 7, 4),
        "Dra. Camila Andrade",
        "CRM 55214 RJ",
        "MARIA",
        LocalDate.of(2026, 10, 2),
        false,
        "Investigação de fadiga",
        List.of(new ClinicalDocumentDetail.ExamItemView("Hemograma Completo", "40304361")),
        null,
        null,
        null,
        List.of(),
        null,
        null,
        null,
        null);
  }

  private static String extractText(byte[] pdf) throws Exception {
    PdfReader reader = new PdfReader(pdf);
    try {
      return new PdfTextExtractor(reader).getTextFromPage(1);
    } finally {
      reader.close();
    }
  }
}
