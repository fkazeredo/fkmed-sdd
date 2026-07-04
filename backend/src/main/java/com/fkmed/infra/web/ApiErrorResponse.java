package com.fkmed.infra.web;

import java.util.List;

/**
 * The predictable error envelope of every API error (docs/architecture/backend.md §Errors): {@code
 * code} is the stable machine-readable error code (== i18n key), {@code message} the localized
 * human message, {@code fields} the per-field validation errors when applicable.
 */
public record ApiErrorResponse(String code, String message, List<FieldViolation> fields) {

  public ApiErrorResponse(String code, String message) {
    this(code, message, List.of());
  }

  /** A single field validation failure. */
  public record FieldViolation(String field, String message) {}
}
