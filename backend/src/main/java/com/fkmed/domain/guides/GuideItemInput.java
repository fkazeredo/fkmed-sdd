package com.fkmed.domain.guides;

/**
 * One requested item of a new guide (SPEC-0018 BR5): the TUSS code, its description and the
 * requested quantity. Starts life as {@link GuideItemStatus#EM_ANALISE}.
 */
public record GuideItemInput(String tussCode, String description, int quantity) {}
