package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0009 BR7/BR9/BR10/BR12: the appointment entity's transitions and REALIZADO derivation. */
class AppointmentTest {

  private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
  private static final UUID BENEFICIARY = UUID.randomUUID();
  private static final UUID UNIT = UUID.randomUUID();
  private static final UUID SLOT = UUID.randomUUID();

  private static Appointment upcomingConsultation() {
    return Appointment.consultation(
        BENEFICIARY,
        "CARDIOLOGIA",
        UNIT,
        SLOT,
        NOW.plus(Duration.ofDays(1)),
        "AG-20260706-0001",
        null,
        NOW);
  }

  @Test
  void newConsultation_isAgendado_andExposesTheSpecialtyAsScope() {
    Appointment appointment = upcomingConsultation();
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.AGENDADO);
    assertThat(appointment.getType()).isEqualTo(AppointmentType.CONSULTATION);
    assertThat(appointment.scopeCode()).isEqualTo("CARDIOLOGIA");
    assertThat(appointment.effectiveStatus(NOW)).isEqualTo(AppointmentStatus.AGENDADO);
  }

  @Test
  void effectiveStatus_becomesRealised_onceStartInstantPassed_withoutPersistingIt() {
    Appointment appointment = upcomingConsultation();
    Instant afterStart = NOW.plus(Duration.ofDays(2));
    assertThat(appointment.effectiveStatus(afterStart)).isEqualTo(AppointmentStatus.REALIZADO);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.AGENDADO); // never stored
  }

  @Test
  void cancel_whileUpcoming_movesToCancelado_keepingReason() {
    Appointment appointment = upcomingConsultation();
    appointment.cancel("desisti", NOW);
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELADO);
    assertThat(appointment.getCancelReason()).isEqualTo("desisti");
  }

  @Test
  void cancel_afterStart_isTooLate() {
    Appointment appointment = upcomingConsultation();
    Instant afterStart = NOW.plus(Duration.ofDays(2));
    assertThatThrownBy(() -> appointment.cancel(null, afterStart))
        .isInstanceOf(AppointmentTooLateException.class);
  }

  @Test
  void reschedule_keepsProtocol_movesSlotAndInstant_andSetsReagendado() {
    Appointment appointment = upcomingConsultation();
    UUID newSlot = UUID.randomUUID();
    Instant newInstant = NOW.plus(Duration.ofDays(3));

    appointment.rescheduleTo(newSlot, newInstant, NOW);

    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.REAGENDADO);
    assertThat(appointment.getProtocol()).isEqualTo("AG-20260706-0001");
    assertThat(appointment.getSlotId()).isEqualTo(newSlot);
    assertThat(appointment.getScheduledAt()).isEqualTo(newInstant);
  }

  @Test
  void reschedule_afterStart_isTooLate() {
    Appointment appointment = upcomingConsultation();
    Instant afterStart = NOW.plus(Duration.ofDays(2));
    assertThatThrownBy(
            () ->
                appointment.rescheduleTo(
                    UUID.randomUUID(), NOW.plus(Duration.ofDays(3)), afterStart))
        .isInstanceOf(AppointmentTooLateException.class);
  }
}
