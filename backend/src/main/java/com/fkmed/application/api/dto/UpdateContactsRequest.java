package com.fkmed.application.api.dto;

import com.fkmed.domain.plan.ContactUpdate;
import jakarta.validation.constraints.Size;

/**
 * Partial contact update (SPEC-0006 PATCH /contacts). PATCH semantics: an absent JSON key
 * deserializes to {@code null} = "keep current"; an empty string = "sent as empty" (clear an
 * optional field, or a rejected attempt to empty a mandatory one). Semantic validation (mandatory
 * fields, phone/CEP/UF formats) lives in the domain so the exact SPEC-0006 error codes are
 * returned; the {@link Size} caps here only guard the free-text address lengths at the boundary.
 */
public record UpdateContactsRequest(
    String contactEmail,
    String mobile,
    String landline,
    String cep,
    @Size(max = 120) String street,
    @Size(max = 10) String number,
    @Size(max = 60) String complement,
    @Size(max = 80) String neighborhood,
    @Size(max = 80) String city,
    String uf) {

  /** Maps to the domain partial-update value. */
  public ContactUpdate toDomain() {
    return new ContactUpdate(
        contactEmail, mobile, landline, cep, street, number, complement, neighborhood, city, uf);
  }
}
