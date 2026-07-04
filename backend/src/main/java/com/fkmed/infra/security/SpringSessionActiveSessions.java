package com.fkmed.infra.security;

import com.fkmed.domain.identity.ActiveSessions;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * {@link ActiveSessions} adapter over Spring Session JDBC (SPEC-0002 BR10). Form-login sessions are
 * indexed by principal name (the login e-mail) in {@code spring_session}; this looks them up via
 * {@link FindByIndexNameSessionRepository#findByPrincipalName} and deletes each, so a password reset
 * terminates every browser the user is signed into. Runs in the caller's transaction (same
 * datasource), so the deletes commit atomically with the password change.
 */
@Component
@RequiredArgsConstructor
public class SpringSessionActiveSessions implements ActiveSessions {

  private final FindByIndexNameSessionRepository<? extends Session> sessions;

  @Override
  public int terminateAllFor(String email) {
    Map<String, ? extends Session> active = sessions.findByPrincipalName(email);
    active.keySet().forEach(sessions::deleteById);
    return active.size();
  }
}
