package com.fkmed.domain.guides;

import java.util.UUID;

/**
 * A guide's status changed (SPEC-0012 BR8): raised by every operator-driven transition (SPEC-0018)
 * — authorize, partially authorize, deny, cancel, mark executed — never at creation. Carries only
 * the number, the new status and (when denied) the reason — never clinical/item detail (BR8), so
 * the notification listener stays free of sensitive content.
 */
public record GuideStatusChanged(
    UUID guideId, UUID beneficiaryId, String number, GuideStatus newStatus, String denialReason) {}
