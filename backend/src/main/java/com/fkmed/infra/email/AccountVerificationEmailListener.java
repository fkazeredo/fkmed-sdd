package com.fkmed.infra.email;

import com.fkmed.domain.identity.AccountCreated;
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
 * Identity-scoped verification-e-mail seam (ADR-0004; SPEC-0004 will centralize it). Listens to
 * {@link AccountCreated} AFTER_COMMIT (baseline §0009) and sends the 24h verification link via the
 * {@link MailSender} port. Best-effort: a mail outage is logged, never propagated — the account was
 * already created and committed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountVerificationEmailListener {

  private static final Locale PRODUCT_LOCALE = Locale.forLanguageTag("pt-BR");

  private final MailSender mailSender;
  private final MessageSource messageSource;
  private final AppIdentityProperties properties;

  /** Builds the verification link and sends it; failures are swallowed after logging. */
  @TransactionalEventListener
  public void onAccountCreated(AccountCreated event) {
    String link =
        properties.verificationBaseUrl()
            + "/verificar-email?token="
            + URLEncoder.encode(event.verificationToken(), StandardCharsets.UTF_8);
    String subject = messageSource.getMessage("email.verification.subject", null, PRODUCT_LOCALE);
    String body =
        messageSource.getMessage("email.verification.body", new Object[] {link}, PRODUCT_LOCALE);
    try {
      mailSender.send(new MailMessage(event.email(), subject, body));
      log.info("verification e-mail dispatched for account {}", event.accountId());
    } catch (RuntimeException e) {
      log.error("failed to send verification e-mail for account {}", event.accountId(), e);
    }
  }
}
