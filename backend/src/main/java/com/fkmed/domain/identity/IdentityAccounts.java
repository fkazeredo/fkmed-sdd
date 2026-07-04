package com.fkmed.domain.identity;

import com.fkmed.domain.plan.Beneficiaries;
import com.fkmed.domain.plan.BeneficiaryMatch;
import java.util.Optional;
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

  /** Credentials for the given login e-mail, if an account exists. */
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
                    account.getBeneficiaryId()));
  }

  /** The 9-digit beneficiary card bound to the account's login e-mail (for the JWT claim). */
  public Optional<String> beneficiaryCardFor(String email) {
    return findByEmail(email)
        .flatMap(credentials -> beneficiaries.findById(credentials.beneficiaryId()))
        .map(BeneficiaryMatch::cardNumber);
  }
}
