package com.fkmed.domain.support;

import java.util.List;

/** The antifraud section content (SPEC-0014 BR3): title, message and the best-practice list. */
public record AntifraudView(String title, String message, List<String> bestPractices) {}
