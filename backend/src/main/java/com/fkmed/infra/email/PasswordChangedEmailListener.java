package com.fkmed.infra.email;

import com.fkmed.domain.identity.PasswordChanged;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Identity-scoped password-changed-notice seam (ADR-0004; SPEC-0004 will centralize it). Listens to
 * {@link PasswordChanged} AFTER_COMMIT (baseline §0009) and sends the "se não foi você, contate os
 * canais" security notice via the {@link MailSender} port, for both the recovery reset and the
 * authenticated change. Best-effort: a mail outage is logged, never propagated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordChangedEmailListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final MailSender mailSender;
  private final MessageSource messageSource;

  /** Sends the change notice; failures are swallowed after logging. */
  @TransactionalEventListener
  public void onPasswordChanged(PasswordChanged event) {
    String subject =
        messageSource.getMessage("email.password-changed.subject", null, PRODUCT_LOCALE);
    String body = messageSource.getMessage("email.password-changed.body", null, PRODUCT_LOCALE);
    try {
      mailSender.send(new MailMessage(event.email(), subject, body));
      log.info("password-changed notice dispatched for account {}", event.accountId());
    } catch (RuntimeException e) {
      log.error("failed to send password-changed notice for account {}", event.accountId(), e);
    }
  }
}
