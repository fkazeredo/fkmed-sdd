package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SPEC-0009 BR11: the appointment lifecycle state machine and its allowed transitions. */
class AppointmentStatusTest {

  @Test
  void agendado_isActive_andMayTransitionToRescheduledCancelledOrRealised() {
    assertThat(AppointmentStatus.AGENDADO.isActive()).isTrue();
    assertThat(AppointmentStatus.AGENDADO.canTransitionTo(AppointmentStatus.REAGENDADO)).isTrue();
    assertThat(AppointmentStatus.AGENDADO.canTransitionTo(AppointmentStatus.CANCELADO)).isTrue();
    assertThat(AppointmentStatus.AGENDADO.canTransitionTo(AppointmentStatus.REALIZADO)).isTrue();
  }

  @Test
  void reagendado_isActive_butNeverBackToAgendado() {
    assertThat(AppointmentStatus.REAGENDADO.isActive()).isTrue();
    assertThat(AppointmentStatus.REAGENDADO.canTransitionTo(AppointmentStatus.CANCELADO)).isTrue();
    assertThat(AppointmentStatus.REAGENDADO.canTransitionTo(AppointmentStatus.REALIZADO)).isTrue();
    assertThat(AppointmentStatus.REAGENDADO.canTransitionTo(AppointmentStatus.AGENDADO)).isFalse();
  }

  @Test
  void cancelledAndRealised_areFinal_andInactive() {
    assertThat(AppointmentStatus.CANCELADO.isActive()).isFalse();
    assertThat(AppointmentStatus.REALIZADO.isActive()).isFalse();
    for (AppointmentStatus target : AppointmentStatus.values()) {
      assertThat(AppointmentStatus.CANCELADO.canTransitionTo(target)).isFalse();
      assertThat(AppointmentStatus.REALIZADO.canTransitionTo(target)).isFalse();
    }
  }
}
