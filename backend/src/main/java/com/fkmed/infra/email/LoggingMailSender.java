package com.fkmed.infra.email;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback {@link MailSender} used when no SMTP host is configured (DECISIONS-BASELINE §0007: the
 * system works without a mail server). Logs a clear "not configured" notice with the recipient and
 * subject only — never message contents that could carry a link/token. Constructed by {@link
 * MailConfig}.
 */
@Slf4j
class LoggingMailSender implements MailSender {

  @Override
  public void send(MailMessage message) {
    log.warn(
        "e-mail not sent (no SMTP host configured): to={} subject='{}'",
        message.to(),
        message.subject());
  }
}
