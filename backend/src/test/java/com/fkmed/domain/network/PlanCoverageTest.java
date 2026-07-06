package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0008 BR4/DL-0014: the plan-coverage filter (ESTADUAL restricts to one UF, NACIONAL/none).
 */
class PlanCoverageTest {

  @Test
  void estadualCoverage_allowsOnlyItsOwnUf() {
    PlanCoverage rj = new PlanCoverage("RJ");
    assertThat(rj.allowsUf("RJ")).isTrue();
    assertThat(rj.allowsUf("rj")).isTrue();
    assertThat(rj.allowsUf("SP")).isFalse();
    assertThat(rj.allowsEveryUf()).isFalse();
    assertThat(rj.singleUf()).isEqualTo(Optional.of("RJ"));
  }

  @Test
  void nacionalCoverage_null_allowsEveryUf() {
    PlanCoverage nacional = new PlanCoverage(null);
    assertThat(nacional.allowsUf("RJ")).isTrue();
    assertThat(nacional.allowsUf("SP")).isTrue();
    assertThat(nacional.allowsEveryUf()).isTrue();
    assertThat(nacional.singleUf()).isEmpty();
  }

  @Test
  void none_noResolvablePlan_deniesEveryUf() {
    assertThat(PlanCoverage.NONE.allowsUf("RJ")).isFalse();
    assertThat(PlanCoverage.NONE.allowsUf(null)).isFalse();
    assertThat(PlanCoverage.NONE.allowsEveryUf()).isFalse();
    assertThat(PlanCoverage.NONE.singleUf()).isEmpty();
  }
}
