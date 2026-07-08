package com.fkmed.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

/** SPEC-0003 BR8: authentication observability keeps personal data masked. */
@ExtendWith(OutputCaptureExtension.class)
class AuthenticationEventsLoggerTest {

  private final AuthenticationEventsLogger logger = new AuthenticationEventsLogger();

  @Test
  void successLog_masksTheEmailHint(CapturedOutput output) {
    logger.onSuccess(
        new AuthenticationSuccessEvent(
            new TestingAuthenticationToken("maria@fkmed.local", "ignored")));

    assertThat(output).contains("login.success user=m***@fkmed.local");
    assertThat(output).doesNotContain("user=maria@fkmed.local");
  }

  @Test
  void failureLog_masksTheEmailHint(CapturedOutput output) {
    logger.onFailure(
        new AuthenticationFailureBadCredentialsEvent(
            new TestingAuthenticationToken("maria@fkmed.local", "bad"),
            new BadCredentialsException("bad")));

    assertThat(output).contains("login.failure user=m***@fkmed.local");
    assertThat(output).doesNotContain("user=maria@fkmed.local");
  }
}
