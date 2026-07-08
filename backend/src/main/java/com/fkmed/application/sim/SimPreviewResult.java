package com.fkmed.application.sim;

import com.fkmed.domain.reimbursement.PreviewSituation;
import java.math.BigDecimal;
import java.util.UUID;

/** Operator-sim response for preview conclusion. */
public record SimPreviewResult(
    UUID id, String protocol, PreviewSituation situation, BigDecimal estimatedValue) {}
