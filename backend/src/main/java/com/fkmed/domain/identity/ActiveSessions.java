package com.fkmed.domain.identity;

/**
 * Port for terminating a user's active login sessions (SPEC-0002 BR10). Defined in the domain and
 * implemented by an infra adapter over Spring Session JDBC, so {@link IdentityService} can enforce
 * "a successful reset MUST terminate all active sessions of the user" without importing the session
 * machinery.
 */
public interface ActiveSessions {

  /**
   * Terminates every active session of the account with the given login e-mail (its Spring Security
   * principal name).
   *
   * @return how many sessions were terminated (0 when the user had none).
   */
  int terminateAllFor(String email);
}
