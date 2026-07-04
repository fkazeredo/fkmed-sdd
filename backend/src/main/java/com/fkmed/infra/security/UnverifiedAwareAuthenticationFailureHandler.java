package com.fkmed.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Form-login failure routing (SPEC-0002): an unverified account ({@link DisabledException}) is sent
 * to {@code /login?unverified} to show the specific "verify your e-mail" message + resend
 * affordance (BR6); every other failure goes to the neutral {@code /login?error} (BR7 — never
 * reveals which credential failed).
 */
@Component
public class UnverifiedAwareAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String target = exception instanceof DisabledException ? "/login?unverified" : "/login?error";
    redirectStrategy.sendRedirect(request, response, target);
  }
}
