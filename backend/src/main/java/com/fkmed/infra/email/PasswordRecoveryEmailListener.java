package com.fkmed.infra.email;

import com.fkmed.domain.identity.PasswordRecoveryRequested;
import com.fkmed.infra.identity.AppIdentityProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Identity-scoped password-recovery-e-mail seam (ADR-0004; SPEC-0004 will centralize it). Listens
 * to {@link PasswordRecoveryRequested} AFTER_COMMIT (baseline §0009) and sends the 30-minute reset
 * link via the {@link MailSender} port. Best-effort: a mail outage is logged, never propagated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryEmailListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final MailSender mailSender;
  private final MessageSource messageSource;
  private final AppIdentityProperties properties;

  /** Builds the reset link and sends it; failures are swallowed after logging. */
  @TransactionalEventListener
  public void onPasswordRecoveryRequested(PasswordRecoveryRequested event) {
    String link =
        properties.verificationBaseUrl()
            + "/redefinir-senha?token="
            + URLEncoder.encode(event.resetToken(), StandardCharsets.UTF_8);
    String subject =
        messageSource.getMessage("email.password-recovery.subject", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage(
            "email.password-recovery.body", new Object[] {link}, PRODUCT_LOCALE);
    try {
      mailSender.send(new MailMessage(event.email(), subject, body));
      log.info("password-recovery e-mail dispatched for account {}", event.accountId());
    } catch (RuntimeException e) {
      log.error("failed to send password-recovery e-mail for account {}", event.accountId(), e);
    }
  }
}
