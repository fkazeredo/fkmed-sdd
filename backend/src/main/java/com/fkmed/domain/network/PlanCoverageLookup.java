package com.fkmed.domain.network;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller's {@link PlanCoverage} (SPEC-0008 BR4, DL-0014) for network search.
 *
 * <p>Reads the {@code beneficiary}/{@code plan} tables directly by SQL rather than through {@code
 * domain.plan}'s Java API: those tables are owned by another module, but {@code
 * docs/architecture/persistence.md} explicitly allows a module to read shared data for
 * "reports/projections" in this single-database monolith — this is exactly that (a read-only,
 * two-column coverage projection, never a command), and it keeps {@code domain.network} from
 * importing {@code domain.plan}'s entities/repositories (which stay module-internal). No dependency
 * on {@code domain.plan}'s Java types is introduced.
 */
@Component
@RequiredArgsConstructor
class PlanCoverageLookup {

  private static final String QUERY =
      "select pl.coverage_uf from beneficiary b "
          + "join plan pl on pl.id = b.plan_id "
          + "where b.card_number = ? and b.active = true";

  private final JdbcTemplate jdbc;

  /**
   * The plan coverage of the active beneficiary holding {@code cardNumber}; empty when the card is
   * absent, blank or matches no active beneficiary (no plan resolvable).
   */
  Optional<PlanCoverage> coverageForCard(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      return Optional.empty();
    }
    // Not `rows.stream().findFirst()`: a NACIONAL plan legitimately maps to a null coverage_uf
    // (DL-0014), and `Stream.findFirst()` throws NPE on a stream whose sole element is null.
    List<String> rows = jdbc.query(QUERY, (rs, rowNum) -> rs.getString("coverage_uf"), cardNumber);
    return rows.isEmpty() ? Optional.empty() : Optional.of(new PlanCoverage(rows.get(0)));
  }
}
