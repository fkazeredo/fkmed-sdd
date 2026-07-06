package com.fkmed.domain.clinicaldocs;

import java.util.UUID;

/**
 * A minimal reference to a document issued in a telemedicine session (SPEC-0010 BR9/BR10): its id
 * and type name. Exposed by {@link ClinicalDocuments#issuedForSession} so the telemedicine room's
 * closure summary can list the issued documents with a "Ver em Minha Saúde" link, without leaking
 * the {@code ClinicalDocument} entity across the module boundary.
 */
public record IssuedDocumentSummary(UUID id, String type) {}
