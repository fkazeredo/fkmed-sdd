package com.fkmed.application.sim;

import com.fkmed.domain.guides.GuideStatus;
import java.util.UUID;

/**
 * The result of an operator guide action (SPEC-0018 BR5): the affected guide's id, number and
 * status.
 */
public record SimGuideResult(UUID id, String number, GuideStatus status) {}
