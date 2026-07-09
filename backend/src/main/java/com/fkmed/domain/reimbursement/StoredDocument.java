package com.fkmed.domain.reimbursement;

/** Validated document metadata after its bytes have been written through the storage port. */
record StoredDocument(
    DocumentCategory category,
    String storageReference,
    String contentType,
    String fileName,
    int fileSize) {}
