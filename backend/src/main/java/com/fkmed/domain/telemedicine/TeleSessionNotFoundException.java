package com.fkmed.domain.telemedicine;

import com.fkmed.domain.error.DomainException;

/**
 * There is no active session to read or act on for the caller — including a join for an appointment
 * that is not a joinable teleconsultation (SPEC-0010 §Error Behavior). Existence is never revealed
 * (SPEC-0003 BR2). Maps to {@code 404 tele.session-not-found}.
 */
public class TeleSessionNotFoundException extends DomainException {

  public static final String CODE = "tele.session-not-found";

  public TeleSessionNotFoundException() {
    super(CODE);
  }
}
