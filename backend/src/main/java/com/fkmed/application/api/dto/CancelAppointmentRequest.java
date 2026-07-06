package com.fkmed.application.api.dto;

import jakarta.validation.constraints.Size;

/** Optional cancellation reason, capped at 200 characters (SPEC-0009 BR9 / §Validation). */
public record CancelAppointmentRequest(@Size(max = 200) String reason) {}
