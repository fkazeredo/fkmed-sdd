package com.fkmed.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Form-login failure routing (SPEC-0002): an unverified account ({@link DisabledException}) is sent
 * to {@code /login?unverified} to show the specific "verify your e-mail" message + resend affordance
 * (BR6); a locked account ({@link LockedException}, BR8) is sent to {@code /login?locked} to show
 * the "conta bloqueada" message — a distinct state raised even with the correct password; every
 * other failure goes to the neutral {@code /login?error} (BR7 — never reveals whether the e-mail
 * exists or which credential failed).
 */
@Component
public class UnverifiedAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String target = targetFor(exception);
    redirectStrategy.sendRedirect(request, response, target);
  }

  private static String targetFor(AuthenticationException exception) {
    if (exception instanceof LockedException) {
      return "/login?locked";
    }
    if (exception instanceof DisabledException) {
      return "/login?unverified";
    }
    return "/login?error";
  }
}
