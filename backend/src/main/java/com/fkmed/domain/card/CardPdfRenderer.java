package com.fkmed.domain.card;

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

/**
 * Renders the digital card as a downloadable PDF (SPEC-0007 BR3): a card-shaped panel laid on an A4
 * page (DL-0009) followed by the full data sheet — name, plan (category + name), card number, CNS
 * in full, ANS registration, coverage and additives. Pure, deterministic rendering over a
 * third-party library (OpenPDF) with no external I/O, so no port/adapter seam is warranted (Rule
 * Zero, ADR-0010).
 */
final class CardPdfRenderer {

  private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font CARD_CATEGORY_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
  private static final Font CARD_NAME_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
  private static final Font CARD_FIELD_FONT = new Font(Font.HELVETICA, 12);
  private static final Font SHEET_LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font SHEET_VALUE_FONT = new Font(Font.HELVETICA, 11);

  private CardPdfRenderer() {}

  /**
   * @throws IllegalStateException when OpenPDF fails to assemble the document — never expected in
   *     practice (no external I/O, deterministic input), but never silently swallowed either.
   */
  static byte[] render(CardResponse card) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 56, 56, 72, 56);
    try {
      PdfWriter.getInstance(document, out);
      document.open();
      document.add(title());
      document.add(cardPanel(card));
      document.add(dataSheet(card));
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("failed to render the digital-card PDF", e);
    }
    return out.toByteArray();
  }

  private static Paragraph title() {
    Paragraph title = new Paragraph("FKMed — Carteirinha Digital", TITLE_FONT);
    title.setSpacingAfter(18f);
    return title;
  }

  /** The card face (DL-0009: card format laid on the A4 page) — name, plan and card number. */
  private static PdfPTable cardPanel(CardResponse card) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    PdfPCell cell = new PdfPCell();
    cell.setPadding(16f);
    cell.setBorderWidth(1.5f);
    cell.addElement(
        new Paragraph(card.planCategory() + " — " + card.planName(), CARD_CATEGORY_FONT));
    cell.addElement(new Paragraph(card.fullName(), CARD_NAME_FONT));
    cell.addElement(new Paragraph("Nº da carteirinha: " + card.cardNumber(), CARD_FIELD_FONT));
    cell.addElement(new Paragraph("Cobertura: " + card.coverage(), CARD_FIELD_FONT));
    table.addCell(cell);
    return table;
  }

  /** The BR3 data sheet: CNS in full, ANS registration, coverage and additives. */
  private static Paragraph dataSheet(CardResponse card) {
    Paragraph sheet = new Paragraph();
    sheet.setSpacingBefore(24f);
    addField(sheet, "CNS: ", card.cns());
    addField(sheet, "Registro ANS: ", card.ansRegistration());
    addField(sheet, "Cobertura: ", card.coverage());
    addField(sheet, "Aditivos: ", String.join(", ", card.additives()));
    return sheet;
  }

  private static void addField(Paragraph sheet, String label, String value) {
    sheet.add(new Chunk(label, SHEET_LABEL_FONT));
    sheet.add(new Chunk(value + "\n", SHEET_VALUE_FONT));
  }
}
