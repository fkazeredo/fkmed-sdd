package com.fkmed.domain.support;

import java.util.UUID;

/** One FAQ question (SPEC-0014 BR5) — {@code category} is a {@link FaqCategoryCodes} code. */
public record FaqEntryView(UUID id, String category, String question, String answer, int order) {}
