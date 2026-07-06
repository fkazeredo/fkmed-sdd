package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.appointment.AppointmentService;
import com.fkmed.domain.appointment.AppointmentType;
import com.fkmed.domain.appointment.BookAppointmentCommand;
import com.fkmed.domain.appointment.BookingConfirmation;
import com.fkmed.domain.appointment.SlotUnavailableException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0009 AC3/BR6 — the central concurrency guarantee: when two callers confirm the last seat of
 * a slot at the same instant, exactly ONE succeeds and the other is told the slot filled, fail-fast
 * (no retry). Exercised over the real transactional path against Testcontainers Postgres with two
 * live threads racing on a capacity-1 slot, so the optimistic {@code @Version} lock on {@link
 * com.fkmed.domain.appointment.ScheduleSlot} is genuinely put under contention.
 */
class AppointmentConcurrencyIT extends AbstractIntegrationTest {

  private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

  private static final String MARIA_CARD = "001234567";
  private static final UUID MARIA_ID = UUID.fromString("3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c");
  private static final UUID PEDRO_ID = UUID.fromString("9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

  private static final UUID FIX_UNIT = UUID.fromString("bb000000-0000-4000-8000-000000000001");
  private static final UUID FIX_AGENDA = UUID.fromString("bb000000-0000-4000-8000-000000000002");
  private static final UUID FIX_SLOT = UUID.fromString("bb000000-0000-4000-8000-000000000003");

  @Autowired private AppointmentService appointments;
  @Autowired private JdbcTemplate jdbc;

  private LocalDateTime slot;

  @BeforeEach
  void setUp() {
    // Isolation (docs/architecture/testing.md): clean in @BeforeEach — Postgres is a shared
    // singleton. Tests build their own capacity-1 fixture rather than the capacity-5 seed.
    cleanBeneficiaryAppointments();
    jdbc.update("delete from schedule_slot where id = ?::uuid", FIX_SLOT);
    jdbc.update("delete from unit_agenda where id = ?::uuid", FIX_AGENDA);
    jdbc.update("delete from care_unit where id = ?::uuid", FIX_UNIT);

    jdbc.update(
        "insert into care_unit (id, name, neighborhood, city, uf, phone)"
            + " values (?::uuid, 'Unidade Corrida', 'Centro', 'Rio de Janeiro', 'RJ', '(21) 3333-9000')",
        FIX_UNIT);
    jdbc.update(
        "insert into unit_agenda (id, unit_id, scope_type, scope_code)"
            + " values (?::uuid, ?::uuid, 'CONSULTATION', 'CARDIOLOGIA')",
        FIX_AGENDA,
        FIX_UNIT);
    LocalDate date = LocalDate.now(CLINIC_ZONE).plusDays(3);
    slot = date.atTime(LocalTime.of(9, 0));
    jdbc.update(
        "insert into schedule_slot (id, agenda_id, slot_date, slot_time, capacity, occupied, version)"
            + " values (?::uuid, ?::uuid, ?, ?, 1, 0, 0)",
        FIX_SLOT,
        FIX_AGENDA,
        date,
        LocalTime.of(9, 0));
  }

  @Test
  void twoConfirmationsForTheLastSeat_exactlyOneWins_theOtherGetsSlotTaken() throws Exception {
    CyclicBarrier gate = new CyclicBarrier(2);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Outcome> maria = pool.submit(book(gate, MARIA_ID));
      Future<Outcome> pedro = pool.submit(book(gate, PEDRO_ID));
      Outcome a = maria.get();
      Outcome b = pedro.get();

      List<Outcome> won = List.of(a, b).stream().filter(o -> o.confirmation != null).toList();
      List<Outcome> lost = List.of(a, b).stream().filter(o -> o.error != null).toList();

      assertThat(won).as("exactly one confirmation succeeds").hasSize(1);
      assertThat(lost).as("the loser is told the slot filled").hasSize(1);
      assertThat(lost.get(0).error).isInstanceOf(SlotUnavailableException.class);
      assertThat(won.get(0).confirmation.protocol()).matches("^AG-\\d{8}-\\d{4}$");
    } finally {
      pool.shutdownNow();
    }

    assertThat(occupiedSeats()).as("the single seat is taken exactly once").isEqualTo(1);
    assertThat(appointmentsOnFixtureSlot()).as("exactly one appointment persisted").isEqualTo(1);
  }

  private Callable<Outcome> book(CyclicBarrier gate, UUID beneficiaryId) {
    BookAppointmentCommand command =
        new BookAppointmentCommand(
            MARIA_CARD,
            null,
            beneficiaryId,
            AppointmentType.CONSULTATION,
            "CARDIOLOGIA",
            null,
            FIX_UNIT,
            slot);
    return () -> {
      gate.await();
      try {
        return Outcome.won(appointments.book(command, null, null));
      } catch (RuntimeException e) {
        return Outcome.lost(e);
      }
    };
  }

  private int occupiedSeats() {
    return jdbc.queryForObject(
        "select occupied from schedule_slot where id = ?::uuid", Integer.class, FIX_SLOT);
  }

  private long appointmentsOnFixtureSlot() {
    return jdbc.queryForObject(
        "select count(*) from appointment where slot_id = ?::uuid", Long.class, FIX_SLOT);
  }

  private void cleanBeneficiaryAppointments() {
    jdbc.update(
        "delete from appointment_attachment where appointment_id in"
            + " (select id from appointment where beneficiary_id in (?::uuid, ?::uuid))",
        MARIA_ID,
        PEDRO_ID);
    jdbc.update(
        "delete from appointment where beneficiary_id in (?::uuid, ?::uuid)", MARIA_ID, PEDRO_ID);
  }

  private record Outcome(BookingConfirmation confirmation, Throwable error) {
    static Outcome won(BookingConfirmation confirmation) {
      return new Outcome(confirmation, null);
    }

    static Outcome lost(Throwable error) {
      return new Outcome(null, error);
    }
  }
}
