package com.fkmed.domain.network;

import java.time.LocalDate;
import java.util.List;

/**
 * A network search result (funnel or name search — SPEC-0008 BR7): the query's {@code
 * referenceDate} (today, product timezone) plus the matching provider cards.
 */
public record ProviderSearchResponse(LocalDate referenceDate, List<ProviderCard> items) {}
