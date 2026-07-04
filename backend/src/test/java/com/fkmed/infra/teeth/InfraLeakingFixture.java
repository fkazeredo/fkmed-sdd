package com.fkmed.infra.teeth;

import com.fkmed.application.api.dto.SystemVersionResponse;

/** Planted violation: an "infra" class depending on the delivery layer (teeth §0010). */
@SuppressWarnings("unused")
public class InfraLeakingFixture {

  SystemVersionResponse leak() {
    return new SystemVersionResponse("teeth", "teeth");
  }
}
