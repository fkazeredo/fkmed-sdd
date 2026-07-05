package com.fkmed.domain.identity;

import com.fkmed.domain.plan.Beneficiaries;
import com.fkmed.domain.plan.BeneficiaryMatch;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only public facade of the identity module for the infra security layer (SPEC-0002 real
 * login). Exposes just what authentication and audit need — credentials by e-mail and the linked
 * beneficiary card — without leaking the {@code UserAccount} entity or its repository.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityAccounts {

  private final UserAccountRepository accounts;
  private final Beneficiaries beneficiaries;
  private final Clock clock;

  /** Credentials for the given login e-mail, if an account exists (lock state as of now, BR8). */
  public Optional<AccountCredentials> findByEmail(String email) {
    return accounts
        .findByEmail(Emails.normalize(email))
        .map(
            account ->
                new AccountCredentials(
                    account.getId(),
                    account.getEmail(),
                    account.getPasswordHash(),
                    account.getStatus(),
                    account.getBeneficiaryId(),
                    account.isLocked(clock.instant())));
  }

  /** The 9-digit beneficiary card bound to the account's login e-mail (for the JWT claim). */
  public Optional<String> beneficiaryCardFor(String email) {
    return findByEmail(email)
        .flatMap(credentials -> beneficiaries.findById(credentials.beneficiaryId()))
        .map(BeneficiaryMatch::cardNumber);
  }

  /**
   * The account id linked to the given beneficiary, if one exists. Lets the notification module
   * route a beneficiary-scoped event ({@code ContactDataChanged}, SPEC-0006) to the owning account
   * without leaking the {@code UserAccount} entity or its repository.
   */
  public Optional<UUID> accountIdForBeneficiary(UUID beneficiaryId) {
    return accounts.findByBeneficiaryId(beneficiaryId).map(UserAccount::getId);
  }
}
