package com.fkmed.infra.observability;

import com.fkmed.domain.audit.Masking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Logs login events (SPEC-0001 §Observability). Never logs credentials. */
@Component
@Slf4j
public class AuthenticationEventsLogger {

  @EventListener
  void onSuccess(AuthenticationSuccessEvent event) {
    log.info(
        "login.success user={}",
        Masking.email(String.valueOf(event.getAuthentication().getName())));
  }

  @EventListener
  void onFailure(AbstractAuthenticationFailureEvent event) {
    log.warn(
        "login.failure user={} reason={}",
        Masking.email(String.valueOf(event.getAuthentication().getName())),
        event.getException().getClass().getSimpleName());
  }
}
