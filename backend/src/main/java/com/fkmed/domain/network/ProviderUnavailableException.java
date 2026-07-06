package com.fkmed.domain.network;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a provider detail is requested for an unknown or inactive provider (SPEC-0008 BR13):
 * both cases answer identically so an inactive provider's existence is never distinguishable from a
 * bad id. Mapped to HTTP 410 by {@code infra.web.HttpErrorMapping}.
 */
public class ProviderUnavailableException extends DomainException {

  public static final String CODE = "network.provider-unavailable";

  public ProviderUnavailableException() {
    super(CODE);
  }
}
