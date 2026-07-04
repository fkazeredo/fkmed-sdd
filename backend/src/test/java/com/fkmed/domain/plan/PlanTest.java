package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPEC-0001 BR4: plan invariants (domain/unit layer). */
class PlanTest {

  @Test
  void create_setsEveryContractField() {
    Plan plan =
        Plan.create(
            "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
            "326305",
            "ESTADUAL",
            true,
            true,
            List.of("Urg/emerg Nacional Hr — Assistência"));
    assertThat(plan.getId()).isNotNull();
    assertThat(plan.getName()).isEqualTo("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP");
    assertThat(plan.getAnsRegistration()).isEqualTo("326305");
    assertThat(plan.getCoverage()).isEqualTo("ESTADUAL");
    assertThat(plan.isCopay()).isTrue();
    assertThat(plan.isReimbursement()).isTrue();
    assertThat(plan.getAdditives()).containsExactly("Urg/emerg Nacional Hr — Assistência");
  }

  @Test
  void name_isRequired() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Plan.create(" ", "326305", "ESTADUAL", true, true, List.of()));
  }

  @Test
  void coverage_isRequired() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Plan.create("PLANO", "326305", " ", true, true, List.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"32630", "3263050", "32630X", ""})
  void ansRegistration_mustHave6NumericDigits(String invalidAns) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Plan.create("PLANO", invalidAns, "ESTADUAL", true, true, List.of()));
  }
}
