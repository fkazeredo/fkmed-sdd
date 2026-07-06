package com.fkmed.application.sim;

import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.infra.platform.SimProperties;
import com.fkmed.infra.security.UserContextProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The operator-simulation authorization guard (SPEC-0018 BR2). The {@code /api/sim/**} routes are
 * reachable only by an internal OPERATOR_SIM — a dev-seeded operator credential enumerated in the
 * {@code app.sim.operator-emails} allowlist (invariant 9's dev-credential pattern), never a
 * beneficiary account. A beneficiary calling any sim route is rejected with 403 {@code
 * sim.forbidden}. The routes themselves are absent (404) when {@code app.sim.enabled} is off — the
 * controllers are {@code @ConditionalOnProperty} — so this guard only ever runs when the API is on.
 */
@Component
@RequiredArgsConstructor
public class OperatorSimAccess {

  private final SimProperties simProperties;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /**
   * Enforces the OPERATOR_SIM role and returns the operator's account id to stamp as the audit
   * author (SPEC-0018 BR3), or {@code null} when the seeded account is not present (e.g. cleaned in
   * the IT suite) — authorization stands on the config allowlist, independent of the DB row.
   *
   * @throws SimForbiddenException when the authenticated caller is not a seeded operator.
   */
  public UUID requireOperator() {
    String email = userContext.current().username();
    if (!simProperties.isOperator(email)) {
      throw new SimForbiddenException();
    }
    return accounts.findByEmail(email).map(AccountCredentials::accountId).orElse(null);
  }
}
