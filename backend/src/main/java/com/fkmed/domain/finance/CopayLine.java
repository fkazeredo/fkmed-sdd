package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the copay statement (SPEC-0013 BR5): the usage date, procedure, provider, the family
 * member who used it and the copay amount.
 */
public record CopayLine(
    LocalDate date, String procedure, String provider, String beneficiaryName, BigDecimal amount) {}
