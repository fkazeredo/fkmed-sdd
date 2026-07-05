package com.fkmed.domain.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;

/** SPEC-0007 BR3: the PDF must contain every required field as extractable text. */
class CardPdfRendererTest {

  private static final CardResponse CARD =
      new CardResponse(
          "MARIA CLARA SOUZA LIMA",
          "001234567",
          "700000000000001",
          "326305",
          "ESTADUAL",
          "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
          "Coletivo por Adesão",
          List.of("Urg/emerg Nacional Hr — Assistência"));

  @Test
  void render_producesAValidPdfDocument() {
    byte[] pdf = CardPdfRenderer.render(CARD);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF-");
  }

  @Test
  void render_containsEveryBr3Field_asExtractableText() throws Exception {
    byte[] pdf = CardPdfRenderer.render(CARD);

    PdfReader reader = new PdfReader(pdf);
    try {
      String text = new PdfTextExtractor(reader).getTextFromPage(1);
      assertThat(text).contains("MARIA CLARA SOUZA LIMA");
      assertThat(text).contains("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP");
      assertThat(text).contains("001234567");
      assertThat(text).contains("700000000000001");
      assertThat(text).contains("326305");
      assertThat(text).contains("ESTADUAL");
      assertThat(text).contains("Urg/emerg Nacional Hr — Assistência");
    } finally {
      reader.close();
    }
  }
}
