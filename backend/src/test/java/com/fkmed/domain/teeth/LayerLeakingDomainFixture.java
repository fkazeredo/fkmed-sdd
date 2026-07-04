package com.fkmed.domain.teeth;

import com.fkmed.infra.web.ApiErrorResponse;

/** Planted violation: a "domain" class depending on infra (teeth for baseline §0012). */
@SuppressWarnings("unused")
public class LayerLeakingDomainFixture {

  ApiErrorResponse leak() {
    return new ApiErrorResponse("teeth", "teeth");
  }
}
