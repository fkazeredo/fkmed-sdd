package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * SPEC-0008/DL-0014: a NACIONAL plan's {@code coverage_uf} column is legitimately {@code null}
 * (every UF allowed). Regression for a defect only reachable with a NACIONAL plan in the database:
 * the RJ-only V1 seed never exercised this row shape, so {@code rows.stream().findFirst()} throwing
 * on a stream whose sole element is {@code null} went unnoticed until a NACIONAL-coverage plan (dev
 * seed, SPEC-0008 heavy fixture) hit it in practice.
 */
@ExtendWith(MockitoExtension.class)
class PlanCoverageLookupTest {

  private static final String CARD = "001234567";

  @Mock private JdbcTemplate jdbc;

  @Test
  @SuppressWarnings("unchecked")
  void coverageForCard_ofNacionalPlan_resolvesWithoutThrowing_evenThoughCoverageUfIsNull() {
    when(jdbc.query(anyString(), any(RowMapper.class), eq(CARD)))
        .thenReturn(nullSafeSingletonList());

    Optional<PlanCoverage> coverage = new PlanCoverageLookup(jdbc).coverageForCard(CARD);

    assertThat(coverage).isPresent();
    assertThat(coverage.get().allowsEveryUf()).isTrue();
  }

  @Test
  @SuppressWarnings("unchecked")
  void coverageForCard_ofEstadualPlan_resolvesTheSingleUf() {
    when(jdbc.query(anyString(), any(RowMapper.class), eq(CARD))).thenReturn(List.of("RJ"));

    Optional<PlanCoverage> coverage = new PlanCoverageLookup(jdbc).coverageForCard(CARD);

    assertThat(coverage).isPresent();
    assertThat(coverage.get().allowsUf("RJ")).isTrue();
    assertThat(coverage.get().allowsUf("SP")).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void coverageForCard_whenNoRowMatches_isEmpty() {
    when(jdbc.query(anyString(), any(RowMapper.class), eq(CARD))).thenReturn(List.of());

    assertThat(new PlanCoverageLookup(jdbc).coverageForCard(CARD)).isEmpty();
  }

  // A List<String> containing a single null element -- exactly what the real RowMapper produces
  // for `rs.getString("coverage_uf")` on a NACIONAL plan's row. `List.of(...)` rejects nulls, so
  // this needs an explicit mutable/nulls-allowing list construction.
  private static List<String> nullSafeSingletonList() {
    return java.util.Collections.singletonList(null);
  }
}
