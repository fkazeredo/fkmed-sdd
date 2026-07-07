package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0013 BR3: the second-copy PDF carries the required fields, and a PAID invoice's copy carries
 * the "PAGO" watermark while an open one does not (AC7).
 */
class InvoicePdfRendererTest {

  private static final String LINE = "23793381286000826010494120780301189999000000001";

  private static InvoiceSummary summary(InvoiceStatus status, LocalDate paidAt) {
    return new InvoiceSummary(
        "id-1",
        "Julho/2026",
        LocalDate.of(2026, 7, 16),
        new BigDecimal("489.90"),
        status,
        paidAt,
        null);
  }

  @Test
  void render_producesAValidPdf_withCompetenciaDueDateAmountAndLine() throws Exception {
    byte[] pdf = InvoicePdfRenderer.render(summary(InvoiceStatus.OPEN, null), LINE);

    assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    PdfReader reader = new PdfReader(pdf);
    try {
      String text = new PdfTextExtractor(reader).getTextFromPage(1);
      assertThat(text).contains("Julho/2026");
      assertThat(text).contains("16/07/2026");
      assertThat(text).contains("489,90");
      assertThat(text).contains(LINE);
      assertThat(text).doesNotContain("PAGO");
    } finally {
      reader.close();
    }
  }

  @Test
  void render_ofAPaidInvoice_carriesThePagoWatermark() throws Exception {
    byte[] pdf =
        InvoicePdfRenderer.render(summary(InvoiceStatus.PAID, LocalDate.of(2026, 7, 10)), LINE);

    PdfReader reader = new PdfReader(pdf);
    try {
      assertThat(new PdfTextExtractor(reader).getTextFromPage(1)).contains("PAGO");
    } finally {
      reader.close();
    }
  }
}
