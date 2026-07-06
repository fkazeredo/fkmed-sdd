package com.fkmed.domain.clinicaldocs;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A Minha Saúde list (SPEC-0011 BR2), most-recent-first: one card per document with the fields the
 * hub renders (title comes from {@link #type} on the client, issuer, issue date, beneficiary and
 * the validity badge — {@code validUntil}/{@code expired}).
 */
public record ClinicalDocumentListResponse(List<Item> items) {

  public record Item(
      UUID id,
      ClinicalDocumentType type,
      String professional,
      String crm,
      LocalDate issuedAt,
      String beneficiary,
      LocalDate validUntil,
      boolean expired) {}
}
