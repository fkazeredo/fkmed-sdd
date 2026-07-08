package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.util.UUID;

/** Domain event emitted when an analyzed preview is concluded (SPEC-0017). */
public record PreviewConcluded(
    UUID previewId, UUID beneficiaryId, String protocol, BigDecimal estimatedValue) {}
