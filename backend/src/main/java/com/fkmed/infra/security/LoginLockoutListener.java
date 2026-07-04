package com.fkmed.infra.security;

import com.fkmed.domain.identity.IdentityService;
import com.fkmed.infra.web.HttpRequestMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Drives the lockout counter (SPEC-0002 BR8) off the form-login outcome events. Scoped to {@link
 * UsernamePasswordAuthenticationToken} so per-request resource-server (JWT) authentications never
 * touch it. Only {@link AuthenticationFailureBadCredentialsEvent} increments — the event Spring
 * publishes for BOTH a wrong password on a real account AND a non-existent e-mail (which is hidden
 * as bad credentials): {@link IdentityService#recordFailedLogin} is a no-op for a non-existent
 * e-mail, preserving BR7 neutrality (no row created or touched). A {@code DisabledException}
 * (unverified) or {@code LockedException} (already locked) is deliberately NOT counted.
 */
@Component
@RequiredArgsConstructor
public class LoginLockoutListener {

  private final IdentityService identityService;

  @EventListener
  void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
    if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
      identityService.recordFailedLogin(
          String.valueOf(event.getAuthentication().getName()), HttpRequestMetadata.current());
    }
  }

  @EventListener
  void onSuccess(AuthenticationSuccessEvent event) {
    if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
      identityService.recordSuccessfulLogin(event.getAuthentication().getName());
    }
  }
}
