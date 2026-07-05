package com.fkmed.domain.plan;

/**
 * A partial contact update (SPEC-0006 BR7): each field is nullable and carries PATCH semantics —
 * {@code null} means "not sent, keep the current value"; an empty string means "sent as empty"
 * (clear an optional field, or a rejected attempt to empty a mandatory one — BR6). A concrete value
 * replaces the field after validation.
 *
 * <p>This distinction (absent vs emptied) is why the request maps to nullable fields rather than
 * Optionals unwrapped at the boundary: {@code {"mobile":""}} must reach the domain as an attempt to
 * empty the mandatory mobile (→ {@link MobileRequiredException}), not as "unchanged".
 */
public record ContactUpdate(
    String contactEmail,
    String mobile,
    String landline,
    String cep,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String uf) {}
