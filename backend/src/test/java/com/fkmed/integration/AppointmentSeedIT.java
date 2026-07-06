package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0009 §Persistence: the V16 seed — the two own care units with address, the exam-type
 * registry and the 30-day Mon–Sat agenda. Assertions are filtered to the seeded rows (by name/code)
 * so they stay valid even after sibling ITs add their own capacity fixtures (Postgres is shared).
 */
class AppointmentSeedIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void seedsTwoOwnCareUnitsWithAddress() {
    Long units =
        jdbc.queryForObject(
            "select count(*) from care_unit where name in"
                + " ('FKMed Unidade Centro', 'FKMed Unidade Tijuca')",
            Long.class);
    assertThat(units).isEqualTo(2);

    Long withAddress =
        jdbc.queryForObject(
            "select count(*) from care_unit where name like 'FKMed Unidade%'"
                + " and neighborhood is not null and city is not null and uf is not null",
            Long.class);
    assertThat(withAddress).isEqualTo(2);
  }

  @Test
  void seedsTheExamTypeRegistry() {
    Long expected =
        jdbc.queryForObject(
            "select count(*) from exam_type where code in"
                + " ('HEMOGRAMA', 'RAIO_X', 'ULTRASSONOGRAFIA', 'RESSONANCIA_MAGNETICA', 'TOMOGRAFIA')",
            Long.class);
    assertThat(expected).isEqualTo(5);
  }

  @Test
  void seedsThirtyDayAgenda_monToSat_only() {
    Long cardiologySlots =
        jdbc.queryForObject(
            "select count(*) from schedule_slot ss join unit_agenda a on ss.agenda_id = a.id"
                + " where a.scope_type = 'CONSULTATION' and a.scope_code = 'CARDIOLOGIA'",
            Long.class);
    assertThat(cardiologySlots).isPositive();

    Long sundays =
        jdbc.queryForObject(
            "select count(*) from schedule_slot where extract(isodow from slot_date) = 7",
            Long.class);
    assertThat(sundays).isZero();

    Long outOfHours =
        jdbc.queryForObject(
            "select count(*) from schedule_slot where slot_time < time '08:00'"
                + " or slot_time > time '16:30'",
            Long.class);
    assertThat(outOfHours).isZero();
  }
}
