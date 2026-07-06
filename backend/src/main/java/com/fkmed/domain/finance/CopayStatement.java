package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.util.List;

/**
 * The copay statement for a filter (SPEC-0013 BR5): the entries of the filtered period and their
 * {@code total} — recomputed on every filter change (the total is always the sum of exactly the
 * {@code entries} returned).
 */
public record CopayStatement(List<CopayLine> entries, BigDecimal total) {}
