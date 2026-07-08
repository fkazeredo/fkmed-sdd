package com.fkmed.domain.reimbursement;

import java.time.LocalDate;

/** The current adhesion-term version (SPEC-0015 BR3), shown scrollable at wizard step 1. */
public record AdhesionTermView(String version, LocalDate publishedAt, String body) {}
