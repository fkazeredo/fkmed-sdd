package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0008 BR7/AC2: the result-card locality label format. */
class ProviderTest {

  @Test
  void formatLocality_uppercasesAndJoinsWithEnDash() {
    assertThat(Provider.formatLocality("Centro", "Rio de Janeiro", "RJ"))
        .isEqualTo("CENTRO, RIO DE JANEIRO – RJ");
  }

  @Test
  void formatLocality_upperCasesAccentedNeighborhoodAndMunicipality() {
    assertThat(Provider.formatLocality("Icaraí", "Niterói", "rj"))
        .isEqualTo("ICARAÍ, NITERÓI – RJ");
  }
}
