package com.fkmed.domain.clinicaldocs;

import java.util.UUID;

/**
 * A clinical document was issued (SPEC-0011 §Events, BR3). Published by {@link
 * ClinicalDocuments#issue} inside the issuance transaction; consumed in Wave 2 by {@code
 * domain.notification} (SPEC-0004) to notify the beneficiary — not wired yet in this slice (no
 * listener exists), only the stable event shape is frozen here.
 *
 * @param documentId the issued document's id.
 * @param beneficiaryId the document's owner.
 * @param type the document's type.
 * @param link the API path to the document's detail ({@code /api/clinical-documents/{id}}).
 */
public record ClinicalDocumentIssued(
    UUID documentId, UUID beneficiaryId, ClinicalDocumentType type, String link) {}
