package com.fkmed.application.sim;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Body of POST /api/sim/reimbursement-previews/{id}/conclude. */
public record SimConcludePreviewRequest(@NotNull @Positive BigDecimal estimatedValue) {}
