package com.fkmed.application.teeth;

import com.fkmed.domain.teeth.ModuleInternalFixture;

/** Planted violation: accesses a @ModuleInternal type from outside its module (teeth §0016). */
@SuppressWarnings("unused")
public class CrossModuleAccessFixture {

  void access() {
    new ModuleInternalFixture().touch();
  }
}
