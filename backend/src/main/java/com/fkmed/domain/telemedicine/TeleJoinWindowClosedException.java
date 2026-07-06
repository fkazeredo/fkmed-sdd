package com.fkmed.domain.telemedicine;

import com.fkmed.domain.error.DomainException;

/**
 * "Entrar na consulta" was attempted outside the allowed window — earlier than 10 minutes before
 * the slot or after its end (SPEC-0010 BR14, §Error Behavior). Maps to {@code 409
 * tele.join-window-closed}.
 */
public class TeleJoinWindowClosedException extends DomainException {

  public static final String CODE = "tele.join-window-closed";

  public TeleJoinWindowClosedException() {
    super(CODE);
  }
}
