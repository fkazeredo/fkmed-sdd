package com.fkmed.teeth;

import org.springframework.beans.factory.annotation.Autowired;

/** Planted violation: field and setter injection (teeth). */
@SuppressWarnings("unused")
public class InjectedFixture {

  @Autowired private String fieldInjected;

  @Autowired
  void setSomething(String value) {
    this.fieldInjected = value;
  }
}
