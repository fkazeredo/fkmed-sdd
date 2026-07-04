package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Array;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** SPEC-0001 BR4/BR5: V1 migrates and the canonical seed is present and constrained. */
class MigrationSeedIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v1_seedsTheCanonicalPlan() throws Exception {
    Map<String, Object> plan =
        jdbc.queryForMap("select * from plan where ans_registration = '326305'");
    assertThat(plan.get("name")).isEqualTo("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP");
    assertThat(plan.get("coverage")).isEqualTo("ESTADUAL");
    assertThat(plan.get("copay")).isEqualTo(true);
    assertThat(plan.get("reimbursement")).isEqualTo(true);
    Object[] additives = (Object[]) ((Array) plan.get("additives")).getArray();
    assertThat(additives).containsExactly("Urg/emerg Nacional Hr — Assistência");
  }

  @Test
  void v1_seedsMariaAsTitular_withValidIdentification() {
    Map<String, Object> maria =
        jdbc.queryForMap("select * from beneficiary where card_number = '001234567'");
    assertThat(maria.get("full_name")).isEqualTo("MARIA CLARA SOUZA LIMA");
    assertThat(maria.get("cns")).isEqualTo("700000000000001");
    assertThat(maria.get("cpf")).isEqualTo("52998224725");
    assertThat(((java.sql.Date) maria.get("birth_date")).toLocalDate())
        .isEqualTo(LocalDate.of(1988, 3, 12));
    assertThat(maria.get("role")).isEqualTo("TITULAR");
    assertThat(maria.get("titular_id")).isNull();
    assertThat(maria.get("active")).isEqualTo(true);
  }

  @Test
  void v1_seedsPedroAsDependentOfMaria_born19YearsBeforeSeedDate() {
    Map<String, Object> pedro =
        jdbc.queryForMap("select * from beneficiary where card_number = '001234575'");
    Map<String, Object> maria =
        jdbc.queryForMap("select * from beneficiary where card_number = '001234567'");
    assertThat(pedro.get("full_name")).isEqualTo("PEDRO SOUZA LIMA");
    assertThat(((java.sql.Date) pedro.get("birth_date")).toLocalDate())
        .isEqualTo(LocalDate.of(2007, 5, 20));
    assertThat(pedro.get("role")).isEqualTo("DEPENDENT");
    assertThat(pedro.get("titular_id")).isEqualTo(maria.get("id"));
    assertThat(pedro.get("plan_id")).isEqualTo(maria.get("plan_id"));
  }

  @Test
  void cardNumber_isUniqueAtTheDatabaseLevel() {
    UUID planId = jdbc.queryForObject("select id from plan limit 1", UUID.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into beneficiary (id, plan_id, full_name, cpf, cns, card_number,"
                        + " birth_date, role, titular_id, active) values (?, ?, 'DUP', "
                        + "'39053344705', '700000000000009', '001234567', '2000-01-01', "
                        + "'TITULAR', null, true)",
                    UUID.randomUUID(),
                    planId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void dependentWithoutTitularLink_isRejectedAtTheDatabaseLevel() {
    UUID planId = jdbc.queryForObject("select id from plan limit 1", UUID.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into beneficiary (id, plan_id, full_name, cpf, cns, card_number,"
                        + " birth_date, role, titular_id, active) values (?, ?, 'ORPHAN', "
                        + "'39053344705', '700000000000008', '001234599', '2000-01-01', "
                        + "'DEPENDENT', null, true)",
                    UUID.randomUUID(),
                    planId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
