package com.fkmed.domain.finance;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders the annual debt-settlement declaration PDF (Lei 12.007/2009 — SPEC-0013 BR7): contract,
 * beneficiaries, the competências settled and the issue date. Offered only for a fully-paid base
 * year (the service guards that — this renderer assumes eligibility). Pure OpenPDF rendering
 * (ADR-0007).
 */
final class SettlementDeclarationPdfRenderer {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", PRODUCT_LOCALE);

  private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
  private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11);

  private SettlementDeclarationPdfRenderer() {}

  /**
   * @throws IllegalStateException when OpenPDF fails to assemble the document.
   */
  static byte[] render(
      String titularName,
      String contractId,
      String planName,
      int year,
      List<String> beneficiaries,
      List<String> competencias,
      LocalDate issueDate) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 56, 56, 72, 56);
    try {
      PdfWriter.getInstance(document, out);
      document.open();
      Paragraph title =
          new Paragraph("FKMed — Declaração Anual de Quitação de Débitos", TITLE_FONT);
      title.setSpacingAfter(6f);
      document.add(title);
      Paragraph legal = new Paragraph("Lei nº 12.007/2009 — ano-base " + year, LABEL_FONT);
      legal.setSpacingAfter(16f);
      document.add(legal);
      document.add(contract(titularName, contractId, planName));

      Paragraph statement =
          new Paragraph(
              "Declaramos, para os devidos fins, a quitação integral dos débitos referentes ao"
                  + " ano-base "
                  + year
                  + " do contrato acima identificado.",
              VALUE_FONT);
      statement.setSpacingAfter(16f);
      document.add(statement);

      document.add(section("Beneficiários"));
      for (String beneficiary : beneficiaries) {
        document.add(new Paragraph("• " + beneficiary, VALUE_FONT));
      }
      document.add(section("Competências quitadas"));
      for (String competencia : competencias) {
        document.add(new Paragraph("• " + competencia, VALUE_FONT));
      }

      Paragraph issued = new Paragraph();
      issued.setSpacingBefore(18f);
      issued.add(new Chunk("Emitida em: ", LABEL_FONT));
      issued.add(new Chunk(DATE.format(issueDate), VALUE_FONT));
      document.add(issued);
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("failed to render the settlement declaration PDF", e);
    }
    return out.toByteArray();
  }

  private static PdfPTable contract(String titularName, String contractId, String planName) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingAfter(18f);
    PdfPCell cell = new PdfPCell();
    cell.setPadding(14f);
    cell.setBorderWidth(1f);
    cell.addElement(labelled("Titular: ", titularName));
    cell.addElement(labelled("Contrato (carteirinha): ", contractId));
    cell.addElement(labelled("Plano: ", planName));
    table.addCell(cell);
    return table;
  }

  private static Paragraph section(String title) {
    Paragraph section = new Paragraph(title, SECTION_FONT);
    section.setSpacingBefore(12f);
    section.setSpacingAfter(4f);
    return section;
  }

  private static Paragraph labelled(String label, String value) {
    Paragraph paragraph = new Paragraph();
    paragraph.add(new Chunk(label, LABEL_FONT));
    paragraph.add(new Chunk(value == null ? "" : value, VALUE_FONT));
    return paragraph;
  }
}
