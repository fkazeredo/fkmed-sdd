package com.fkmed.domain.reimbursement;

/**
 * One submitted attachment as received (SPEC-0015 BR8), before persistence. At the controller
 * boundary {@code contentType} may still be the client-declared value; {@link ReimbursementService}
 * replaces it with the magic-byte-detected value before creating the entity.
 */
public record UploadedDocument(
    DocumentCategory category, byte[] content, String contentType, String fileName) {}
