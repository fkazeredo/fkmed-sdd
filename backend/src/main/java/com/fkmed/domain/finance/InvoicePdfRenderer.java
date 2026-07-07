package com.fkmed.domain.finance;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BarcodeInter25;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders an invoice's second copy as a downloadable PDF (SPEC-0013 BR3): competência, due date,
 * amount, the 47-digit digitable line and the barcode. A PAID invoice's copy carries a diagonal
 * "PAGO" watermark (BR3); the copy is generatable in any state. Pure, deterministic rendering over
 * OpenPDF, reusing {@code domain.card}'s setup (ADR-0007) — no external I/O.
 */
final class InvoicePdfRenderer {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", PRODUCT_LOCALE);

  private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font LABEL_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font VALUE_FONT = new Font(Font.HELVETICA, 11);
  private static final Font LINE_FONT = new Font(Font.COURIER, 12, Font.BOLD);
  private static final Font WATERMARK_FONT = new Font(Font.HELVETICA, 96, Font.BOLD, Color.RED);

  private InvoicePdfRenderer() {}

  /**
   * @throws IllegalStateException when OpenPDF fails to assemble the document — never expected in
   *     practice (deterministic input), but never silently swallowed either.
   */
  static byte[] render(InvoiceSummary summary, String digitableLine) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 56, 56, 72, 56);
    try {
      PdfWriter writer = PdfWriter.getInstance(document, out);
      document.open();
      document.add(title());
      document.add(header(summary));
      document.add(digitableLineBlock(digitableLine));
      document.add(barcode(writer, digitableLine));
      if (summary.status() == InvoiceStatus.PAID) {
        stampPaidWatermark(writer);
      }
      document.close();
    } catch (DocumentException e) {
      throw new IllegalStateException("failed to render the invoice PDF", e);
    }
    return out.toByteArray();
  }

  private static Paragraph title() {
    Paragraph title = new Paragraph("FKMed — Boleto (2ª via)", TITLE_FONT);
    title.setSpacingAfter(18f);
    return title;
  }

  private static PdfPTable header(InvoiceSummary summary) {
    PdfPTable table = new PdfPTable(1);
    table.setWidthPercentage(100);
    table.setSpacingAfter(18f);
    PdfPCell cell = new PdfPCell();
    cell.setPadding(14f);
    cell.setBorderWidth(1f);
    addField(cell, "Competência: ", summary.competencia());
    addField(cell, "Vencimento: ", DATE.format(summary.dueDate()));
    addField(cell, "Valor: ", currency(summary.amount()));
    if (summary.status() == InvoiceStatus.OVERDUE && summary.updatedAmount() != null) {
      addField(cell, "Valor atualizado: ", currency(summary.updatedAmount().total()));
    }
    if (summary.status() == InvoiceStatus.PAID && summary.paidAt() != null) {
      addField(cell, "Pago em: ", DATE.format(summary.paidAt()));
    }
    table.addCell(cell);
    return table;
  }

  private static Paragraph digitableLineBlock(String digitableLine) {
    Paragraph block = new Paragraph();
    block.add(new Chunk("Linha digitável\n", LABEL_FONT));
    block.add(new Chunk(digitableLine, LINE_FONT));
    block.setSpacingAfter(18f);
    return block;
  }

  private static Paragraph barcode(PdfWriter writer, String digitableLine) {
    Paragraph block = new Paragraph();
    block.add(new Chunk("Código de barras\n", LABEL_FONT));
    String payload = DigitableLine.barcodeOf(digitableLine);
    try {
      BarcodeInter25 barcode = new BarcodeInter25();
      barcode.setCode(payload);
      barcode.setBarHeight(48f);
      Image image = barcode.createImageWithBarcode(writer.getDirectContent(), null, null);
      block.add(new Chunk(image, 0, 0));
    } catch (RuntimeException fallback) {
      // Never fail the second copy over the barcode image: fall back to the payload digits.
      block.add(new Chunk(payload, LINE_FONT));
    }
    return block;
  }

  private static void stampPaidWatermark(PdfWriter writer) {
    PdfContentByte canvas = writer.getDirectContentUnder();
    canvas.saveState();
    PdfGState state = new PdfGState();
    state.setFillOpacity(0.20f);
    canvas.setGState(state);
    ColumnText.showTextAligned(
        canvas, Element.ALIGN_CENTER, new Phrase("PAGO", WATERMARK_FONT), 297.5f, 421f, 45f);
    canvas.restoreState();
  }

  private static String currency(BigDecimal amount) {
    return NumberFormat.getCurrencyInstance(PRODUCT_LOCALE).format(amount);
  }

  private static void addField(PdfPCell cell, String label, String value) {
    Paragraph field = new Paragraph();
    field.add(new Chunk(label, LABEL_FONT));
    field.add(new Chunk(value, VALUE_FONT));
    cell.addElement(field);
  }
}
