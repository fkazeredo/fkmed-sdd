package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.network.NetworkSpecialties;
import com.fkmed.domain.network.SpecialtyOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SPEC-0008 BR6 + ADR-0011 Wave 2 freeze: the {@link NetworkSpecialties} public facade is the one
 * point of reuse SPEC-0009's {@code domain.appointment} will consume — assert it works against the
 * real V15-seeded registry (Testcontainers Postgres), not just the controller's happy path.
 */
class NetworkSpecialtiesIT extends AbstractIntegrationTest {

  @Autowired private NetworkSpecialties specialties;

  @Test
  void isValid_trueForASeededCode_falseForUnknownOrNull() {
    assertThat(specialties.isValid("CARDIOLOGIA")).isTrue();
    assertThat(specialties.isValid("NOT-A-REAL-SPECIALTY")).isFalse();
    assertThat(specialties.isValid(null)).isFalse();
  }

  @Test
  void all_returnsAtLeast15_alphabeticalByName() {
    List<SpecialtyOption> all = specialties.all();
    assertThat(all.size()).isGreaterThanOrEqualTo(15);
    assertThat(all).extracting(SpecialtyOption::code).contains("CARDIOLOGIA", "PEDIATRIA");
    List<String> names = all.stream().map(SpecialtyOption::name).toList();
    assertThat(names).isSorted();
  }
}
