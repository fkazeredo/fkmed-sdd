package com.fkmed.infra.platform;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the operator-simulation API (SPEC-0018, ADR-0017/DL-0021). Lives in {@code
 * infra} so both the delivery-layer sim guard ({@code application.sim}) and {@link
 * ProdReadinessValidator} ({@code infra.platform}) can read it without an infra→application
 * dependency (ArchUnit §0010).
 *
 * @param enabled whether the {@code /api/sim/**} routes are wired at all (BR1); {@code false} by
 *     default, so the routes are ABSENT (404) unless a dev/e2e profile opts in. The prod profile is
 *     refused this flag by {@link ProdReadinessValidator}.
 * @param operatorEmails the dev-seeded operator login e-mails granted the internal OPERATOR_SIM
 *     role (BR2) — the "dev-credential allowlist" pattern (invariant 9); a beneficiary account is
 *     never in this list, so it is rejected with 403.
 */
@ConfigurationProperties(prefix = "app.sim")
public record SimProperties(
    @DefaultValue("false") boolean enabled, @DefaultValue List<String> operatorEmails) {

  public SimProperties {
    operatorEmails = operatorEmails == null ? List.of() : List.copyOf(operatorEmails);
  }

  /**
   * Whether {@code email} is a seeded operator (case-insensitive) — the OPERATOR_SIM check (BR2).
   */
  public boolean isOperator(String email) {
    return email != null && operatorEmails.stream().anyMatch(email::equalsIgnoreCase);
  }
}
