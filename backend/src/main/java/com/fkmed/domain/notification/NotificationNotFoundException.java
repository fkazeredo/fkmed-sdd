package com.fkmed.domain.notification;

import com.fkmed.domain.error.DomainException;

/**
 * Raised when a notification is unknown or not owned by the caller (SPEC-0004 §Error Behavior —
 * mapped to HTTP 404 by {@code infra.web.HttpErrorMapping}). Existence of another account's
 * notification is never revealed — a foreign id is a plain not-found.
 */
public class NotificationNotFoundException extends DomainException {

  public static final String CODE = "notification.not-found";

  public NotificationNotFoundException() {
    super(CODE);
  }
}
