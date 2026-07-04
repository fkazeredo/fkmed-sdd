package com.fkmed.infra.email;

/**
 * Outbound e-mail port (DECISIONS-BASELINE §0007; ADR-0004). One port, environment-configured
 * adapter (SMTP when {@code spring.mail.host} is set, a logging fallback otherwise), so the system
 * boots and runs without a mail server. SPEC-0004 will centralize delivery (templates, in-app,
 * outbox) behind this same seam.
 */
public interface MailSender {

  /** Sends one plain-text message; may fail — callers treat delivery as best-effort. */
  void send(MailMessage message);
}
