package com.fkmed.infra.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Selects the {@link MailSender} adapter by environment (ADR-0004): the SMTP adapter when {@code
 * spring.mail.host} is set, otherwise the logging fallback — so every profile boots.
 */
@Configuration
public class MailConfig {

  @Bean
  @ConditionalOnProperty(name = "spring.mail.host")
  MailSender smtpMailSender(
      JavaMailSender javaMailSender,
      @Value("${app.mail.from:nao-responder@fkmed.local}") String from) {
    return new SmtpMailSender(javaMailSender, from);
  }

  @Bean
  @ConditionalOnMissingBean(MailSender.class)
  MailSender loggingMailSender() {
    return new LoggingMailSender();
  }
}
