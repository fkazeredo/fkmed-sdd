package com.fkmed.domain.reimbursement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One Terapia/Psicologia session as submitted (SPEC-0015 BR7), before persistence. */
public record SessionInput(LocalDate sessionDate, BigDecimal amount) {}
