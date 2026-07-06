package com.fkmed.domain.support;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade for the FAQ (SPEC-0014 BR5/BR6): category + real-time
 * search filtering, case/accent-insensitive over both the question and the answer. Counts
 * zero-result searches (SPEC-0014 §Observability, a content-gap signal), mirroring the metrics
 * pattern of {@code domain.guides.TokenService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqSearch {

  private final FaqEntryRepository entries;
  private final MeterRegistry metrics;

  /**
   * The active FAQ entries matching {@code category} (exact code match, or every category when
   * blank/{@code null}) and {@code q} (case/accent-insensitive over question + answer, or every
   * entry when blank/{@code null}), in content-defined order.
   */
  public List<FaqQuestionView> search(String category, String q) {
    List<FaqEntry> matches =
        entries.findByActiveTrueOrderByDisplayOrderAsc().stream()
            .filter(e -> category == null || category.isBlank() || e.getCategory().equals(category))
            .filter(e -> e.matchesQuery(q))
            .toList();

    if (matches.isEmpty() && q != null && !q.isBlank()) {
      metrics.counter("support.faq.search.zero-results").increment();
    }

    return matches.stream()
        .map(
            e ->
                new FaqQuestionView(
                    e.getId(),
                    e.getCategory(),
                    e.getQuestion(),
                    e.getAnswer(),
                    e.getDisplayOrder()))
        .toList();
  }
}
