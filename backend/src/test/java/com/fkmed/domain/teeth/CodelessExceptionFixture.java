package com.fkmed.domain.teeth;

import com.fkmed.domain.error.DomainException;

/** Planted violation: a concrete DomainException without the CODE constant (teeth). */
public class CodelessExceptionFixture extends DomainException {

  public CodelessExceptionFixture() {
    super("teeth.codeless");
  }
}
