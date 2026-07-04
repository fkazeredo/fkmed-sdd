package com.fkmed.infra.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * SMTP adapter of {@link MailSender} (ADR-0004): sends through Spring's {@link JavaMailSender}.
 * Active only when {@code spring.mail.host} is configured (dev/E2E point it at Mailpit; production
 * at a real SMTP host). Constructed by {@link MailConfig}.
 */
class SmtpMailSender implements MailSender {

  private final JavaMailSender mailSender;
  private final String from;

  SmtpMailSender(JavaMailSender mailSender, String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  @Override
  public void send(MailMessage message) {
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(from);
    mail.setTo(message.to());
    mail.setSubject(message.subject());
    mail.setText(message.body());
    mailSender.send(mail);
  }
}
