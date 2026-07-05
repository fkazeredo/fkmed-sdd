package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * SPEC-0008 BR5: the specialty filter only ever applies within the specialty-step service type
 * (Consultórios – Clínicas – Terapias); every other type has its funnel specialty step (and any
 * client-sent specialty) cleared server-side.
 */
class ServiceTypeTest {

  @Test
  void specialtyStepApplies_keepsTheSpecialtyFilter() {
    assertThat(ServiceType.clearSpecialtyOutsideItsStep(true, "CARDIOLOGIA"))
        .isEqualTo("CARDIOLOGIA");
    assertThat(ServiceType.clearSpecialtyOutsideItsStep(true, null)).isNull();
  }

  @Test
  void specialtyStepDoesNotApply_clearsAnySpecialty() {
    // e.g. LABORATORIOS_EXAMES/TEA/etc.: a client sending a specialty anyway must not silently
    // filter results by a criterion the service type carries no data for.
    assertThat(ServiceType.clearSpecialtyOutsideItsStep(false, "CARDIOLOGIA")).isNull();
    assertThat(ServiceType.clearSpecialtyOutsideItsStep(false, null)).isNull();
  }
}
