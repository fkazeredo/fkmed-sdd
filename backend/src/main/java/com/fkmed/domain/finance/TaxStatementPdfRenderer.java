package com.fkmed.domain.finance;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Renders the income-tax (IR) statement PDF for one base year (SPEC-0013 BR6): contract
 * identification, the 12 monthly amounts (zeros where none) and the annual total. Pure OpenPDF
 * rendering (ADR-0007).
 */
final class TaxStatementPdfRenderer {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final String[] MONTHS = {
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
  };

  private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11);

  private TaxStatementPdfRenderer() {}

  /**
   * @throws IllegalStateException when OpenPDF fails to assemble the document.
   */
  static byte[] render(
      String titularName, String contractId, String planName, int year, BigDecimal[] months) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 56, 56, 72, 56);
    try {
      PdfWriter.getInstance(document, out);
      document.open();
      Paragraph title = new Paragraph("FKMed — Demonstrativo para Imposto de Renda", TITLE_FONT);
      title.setSpacingAfter(6f);
      document.add(title);
      Paragraph subtitle = new Paragraph("Ano-base " + year, LABEL_FONT);
      subtitle.setSpacingAfter(16f);
      document.add(subtitle);
      document.add(contract(titularName, contractId, planName));
      document.add(monthsTable(year, months));
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("failed to render the IR statement PDF", e);
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

  private static PdfPTable monthsTable(int year, BigDecimal[] months) {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    header(table, "Mês");
    header(table, "Valor pago");
    BigDecimal total = BigDecimal.ZERO.setScale(2);
    for (int i = 0; i < 12; i++) {
      table.addCell(cell(MONTHS[i] + "/" + year, false));
      table.addCell(cell(currency(months[i]), true));
      total = total.add(months[i]);
    }
    table.addCell(cell("Total anual", false, true));
    table.addCell(cell(currency(total), true, true));
    return table;
  }

  private static void header(PdfPTable table, String text) {
    PdfPCell cell = new PdfPCell(new Paragraph(text, LABEL_FONT));
    cell.setPadding(6f);
    table.addCell(cell);
  }

  private static PdfPCell cell(String text, boolean right) {
    return cell(text, right, false);
  }

  private static PdfPCell cell(String text, boolean right, boolean bold) {
    PdfPCell cell = new PdfPCell(new Paragraph(text, bold ? LABEL_FONT : VALUE_FONT));
    cell.setPadding(6f);
    if (right) {
      cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
    }
    return cell;
  }

  private static Paragraph labelled(String label, String value) {
    Paragraph paragraph = new Paragraph();
    paragraph.add(new com.lowagie.text.Chunk(label, LABEL_FONT));
    paragraph.add(new com.lowagie.text.Chunk(value == null ? "" : value, VALUE_FONT));
    return paragraph;
  }

  private static String currency(BigDecimal amount) {
    return NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(amount);
  }
}
