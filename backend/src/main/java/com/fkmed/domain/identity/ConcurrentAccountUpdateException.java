package com.fkmed.domain.identity;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a concurrent modification of the same {@link UserAccount} cannot be reconciled — the
 * bounded retry of {@link IdentityService#recordFailedLogin} was exhausted, or an optimistic-lock
 * conflict surfaced at commit in another account mutation (change/reset password, activation).
 *
 * <p>Débito técnico A (DL-0005): the raw framework {@code ObjectOptimisticLockingFailureException}
 * MUST NOT leak to the client; it is translated into this domain error, mapped to HTTP 409 by
 * {@code infra.web.HttpErrorMapping} so the caller can simply retry the operation.
 */
public class ConcurrentAccountUpdateException extends DomainException {

  public static final String CODE = "auth.concurrent-update";

  public ConcurrentAccountUpdateException() {
    super(CODE);
  }
}
