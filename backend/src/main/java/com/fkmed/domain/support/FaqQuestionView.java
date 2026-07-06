package com.fkmed.domain.support;

import java.util.UUID;

/** One FAQ question matching the current filter (SPEC-0014 BR5), in content-defined order. */
public record FaqQuestionView(
    UUID id, String category, String question, String answer, int displayOrder) {}
