package com.fkmed.application.sim;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/sim/reimbursements/{id}/deny. */
public record SimDenyReimbursementRequest(@NotBlank String reason) {}
