package com.fkmed.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Test config importing a {@link RecordingMailSender} as the primary {@code MailSender}. */
@TestConfiguration
public class RecordingMailConfig {

  @Bean
  @Primary
  RecordingMailSender recordingMailSender() {
    return new RecordingMailSender();
  }
}
