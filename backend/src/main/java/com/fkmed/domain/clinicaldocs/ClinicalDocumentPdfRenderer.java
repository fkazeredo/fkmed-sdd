package com.fkmed.domain.clinicaldocs;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a clinical document's detail as a downloadable, faithful PDF (SPEC-0011 BR7): issuer,
 * CRM, issue date, validity and the type-specific body (exam items + TUSS, referral, medications,
 * or the sick-note period/CID/notes — DL-0020). Pure, deterministic rendering over OpenPDF, reusing
 * {@code domain.card}'s setup (ADR-0007) — no external I/O, so no port/adapter seam is warranted
 * (Rule Zero).
 */
final class ClinicalDocumentPdfRenderer {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", PRODUCT_LOCALE);

  private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
  private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11);

  private static final Map<ClinicalDocumentType, String> TITLES =
      Map.of(
          ClinicalDocumentType.EXAM_ORDER, "Solicitação de Exames",
          ClinicalDocumentType.REFERRAL, "Encaminhamento Médico",
          ClinicalDocumentType.PRESCRIPTION, "Receituário",
          ClinicalDocumentType.SICK_NOTE, "Atestado Médico");

  private ClinicalDocumentPdfRenderer() {}

  /**
   * @throws IllegalStateException when OpenPDF fails to assemble the document — never expected in
   *     practice (no external I/O, deterministic input), but never silently swallowed either.
   */
  static byte[] render(ClinicalDocumentDetail detail) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 56, 56, 72, 56);
    try {
      PdfWriter.getInstance(document, out);
      document.open();
      document.add(title(detail));
      document.add(header(detail));
      document.add(body(detail));
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("failed to render the clinical-document PDF", e);
    }
    return out.toByteArray();
  }

  private static Paragraph title(ClinicalDocumentDetail detail) {
    Paragraph title = new Paragraph("FKMed — " + TITLES.get(detail.type()), TITLE_FONT);
    title.setSpacingAfter(18f);
    return title;
  }

  /** The common header (BR6): professional + CRM, beneficiary, issue date and validity. */
  private static PdfPTable header(ClinicalDocumentDetail detail) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingAfter(18f);
    PdfPCell cell = new PdfPCell();
    cell.setPadding(14f);
    cell.setBorderWidth(1f);
    addField(cell, "Beneficiário: ", detail.beneficiary());
    addField(cell, "Profissional: ", detail.professional());
    addField(cell, "CRM: ", detail.crm());
    addField(cell, "Emitido em: ", DATE.format(detail.issuedAt()));
    addField(
        cell,
        "Validade: ",
        detail.validUntil() == null
            ? "Sem validade"
            : (detail.expired() ? "Expirado em " : "Válido até ")
                + DATE.format(detail.validUntil()));
    table.addCell(cell);
    return table;
  }

  private static Paragraph body(ClinicalDocumentDetail detail) {
    return switch (detail.type()) {
      case EXAM_ORDER -> examOrderBody(detail);
      case REFERRAL -> referralBody(detail);
      case PRESCRIPTION -> prescriptionBody(detail);
      case SICK_NOTE -> sickNoteBody(detail);
    };
  }

  private static Paragraph examOrderBody(ClinicalDocumentDetail detail) {
    Paragraph body = new Paragraph();
    addSection(body, "Indicação Clínica");
    body.add(new Paragraph(detail.clinicalIndication(), VALUE_FONT));
    addSection(body, "Exames Solicitados");
    for (ClinicalDocumentDetail.ExamItemView item : nonNull(detail.exams())) {
      body.add(new Paragraph("• " + item.name() + " (TUSS " + item.tuss() + ")", VALUE_FONT));
    }
    return body;
  }

  private static Paragraph referralBody(ClinicalDocumentDetail detail) {
    Paragraph body = new Paragraph();
    addSection(body, "Especialidade");
    body.add(
        new Paragraph(
            detail.specialtyName() != null ? detail.specialtyName() : detail.specialtyCode(),
            VALUE_FONT));
    addSection(body, "Motivo do Encaminhamento");
    body.add(new Paragraph(detail.reason(), VALUE_FONT));
    return body;
  }

  private static Paragraph prescriptionBody(ClinicalDocumentDetail detail) {
    Paragraph body = new Paragraph();
    addSection(body, "Medicações");
    for (ClinicalDocumentDetail.PrescriptionItemView item : nonNull(detail.medications())) {
      body.add(new Paragraph("• " + item.medication() + " — " + item.posology(), VALUE_FONT));
      if (item.guidance() != null && !item.guidance().isBlank()) {
        body.add(new Paragraph("   " + item.guidance(), VALUE_FONT));
      }
    }
    return body;
  }

  private static Paragraph sickNoteBody(ClinicalDocumentDetail detail) {
    Paragraph body = new Paragraph();
    addSection(body, "Período de Afastamento");
    body.add(
        new Paragraph(
            DATE.format(detail.periodStart()) + " a " + DATE.format(detail.periodEnd()),
            VALUE_FONT));
    addSection(body, "CID");
    body.add(new Paragraph(detail.cid(), VALUE_FONT));
    if (detail.notes() != null && !detail.notes().isBlank()) {
      addSection(body, "Observações");
      body.add(new Paragraph(detail.notes(), VALUE_FONT));
    }
    return body;
  }

  private static void addSection(Paragraph body, String title) {
    Paragraph section = new Paragraph(title, SECTION_FONT);
    section.setSpacingBefore(12f);
    body.add(section);
  }

  private static void addField(PdfPCell cell, String label, String value) {
    Paragraph field = new Paragraph();
    field.add(new Chunk(label, LABEL_FONT));
    field.add(new Chunk(value, VALUE_FONT));
    cell.addElement(field);
  }

  private static <T> List<T> nonNull(List<T> items) {
    return items == null ? List.of() : items;
  }
}
