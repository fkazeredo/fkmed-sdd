package com.fkmed.integration;

import com.fkmed.infra.email.MailMessage;
import com.fkmed.infra.email.MailSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures sent mail so slice-1.2 ITs can read the one-time links they carry (reset link, BR10) and
 * assert the "não foi você" password-changed notice (ADR-0004 seam under test). Wired as the
 * {@code @Primary MailSender} by {@link RecordingMailConfig}.
 */
public final class RecordingMailSender implements MailSender {

  public final List<MailMessage> messages = new CopyOnWriteArrayList<>();

  @Override
  public void send(MailMessage message) {
    messages.add(message);
  }
}
