package com.fkmed.domain.support;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Public facade for the antifraud section (SPEC-0014 BR3): fixed operator copy with no dedicated
 * table (Rule Zero — this is page content, not a channel/contact), the destination of the Home
 * fraud banner (SPEC-0005 BR9/AC6, anchor {@code #antifraude}).
 */
@Service
public class AntifraudContent {

  private static final AntifraudView CONTENT =
      new AntifraudView(
          "Alerta de golpe!",
          "A operadora não solicita dados ou pagamentos por WhatsApp",
          List.of(
              "Nunca compartilhe sua senha ou token de acesso com ninguém.",
              "Valide sempre o boleto antes de pagar.",
              "Utilize somente os canais oficiais desta página para falar com a operadora."));

  /** The fixed antifraud copy (BR3). */
  public AntifraudView content() {
    return CONTENT;
  }
}
