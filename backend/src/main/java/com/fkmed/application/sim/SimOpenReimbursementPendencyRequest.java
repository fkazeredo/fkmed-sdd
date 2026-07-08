package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/sim/reimbursements/{id}/pendency. */
public record SimOpenReimbursementPendencyRequest(@NotBlank String description) {}
