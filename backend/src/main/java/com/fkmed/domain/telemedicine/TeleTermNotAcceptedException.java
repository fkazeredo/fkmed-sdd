package com.fkmed.domain.telemedicine;

import com.fkmed.domain.error.DomainException;

/**
 * The accepted term version is absent or does not equal the current published version (SPEC-0010
 * BR4, §Error Behavior). Maps to {@code 422 tele.term-not-accepted}.
 */
public class TeleTermNotAcceptedException extends DomainException {

  public static final String CODE = "tele.term-not-accepted";

  public TeleTermNotAcceptedException() {
    super(CODE);
  }
}
