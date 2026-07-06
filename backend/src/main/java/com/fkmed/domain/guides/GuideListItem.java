package com.fkmed.domain.guides;

import java.time.LocalDate;
import java.util.UUID;

/** One row of the Minha Guias list (SPEC-0012 BR2), most-recent-first. */
public record GuideListItem(
    UUID id,
    String number,
    GuideType type,
    String requestingProvider,
    LocalDate requestedAt,
    GuideStatus status) {}
