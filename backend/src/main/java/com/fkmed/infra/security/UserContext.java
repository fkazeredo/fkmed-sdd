package com.fkmed.infra.security;

import java.util.Optional;

/**
 * Identity of the authenticated caller as seen by delivery code (no direct {@code
 * SecurityContextHolder} access across the codebase — see {@link UserContextProvider}).
 */
public record UserContext(String username, String beneficiaryCardOrNull) {

  /** The beneficiary card bound to this user, when the user has a beneficiary link. */
  public Optional<String> beneficiaryCard() {
    return Optional.ofNullable(beneficiaryCardOrNull);
  }
}
