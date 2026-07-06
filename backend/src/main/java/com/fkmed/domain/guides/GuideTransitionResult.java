package com.fkmed.domain.guides;

import java.util.UUID;

/**
 * The delivery-safe outcome of a guide create/transition (SPEC-0018 BR5): the operator-sim seam
 * consumes THIS view instead of the {@link Guide} entity, keeping the delivery layer entity-free
 * (docs/architecture/modules-and-apis.md — services return view/response records, never
 * {@code @Entity}). Carries the {@code beneficiaryId} the sim needs for its audit entry.
 */
public record GuideTransitionResult(
    UUID id, UUID beneficiaryId, String number, GuideStatus status) {}
