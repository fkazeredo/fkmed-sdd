package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body of {@code POST /api/sim/finance/copay} (SPEC-0013 §Operator-sim): records a copay charge for
 * a family member's usage.
 */
public record SimCreateCopayRequest(
    @NotNull LocalDate entryDate,
    @NotBlank String procedure,
    @NotBlank String provider,
    @NotNull UUID beneficiaryId,
    @NotNull @Positive BigDecimal amount) {}
