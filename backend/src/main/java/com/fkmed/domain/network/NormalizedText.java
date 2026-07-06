package com.fkmed.domain.network;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Case/accent-insensitive substring matching (SPEC-0008 BR2/BR8): selector filters and the name
 * search must match {@code "cardio"} against {@code "Cardiológica"} regardless of case or
 * diacritics. Pure, no infrastructure — the candidate sets involved (a UF's municipalities, the
 * active provider base) are small enough that filtering in Java is simpler than an indexed database
 * search extension (Rule Zero); revisit with a {@code pg_trgm}/{@code unaccent} index if the
 * network grows enough to matter.
 */
final class NormalizedText {

  private NormalizedText() {}

  /**
   * Whether {@code haystack} contains {@code needle}, ignoring case and diacritics. A {@code null}
   * or blank {@code needle} matches everything (no filter applied); a {@code null} {@code haystack}
   * never matches a non-blank needle.
   */
  static boolean contains(String haystack, String needle) {
    if (needle == null || needle.isBlank()) {
      return true;
    }
    if (haystack == null) {
      return false;
    }
    return normalize(haystack).contains(normalize(needle));
  }

  private static String normalize(String value) {
    String decomposed = Normalizer.normalize(value.strip(), Normalizer.Form.NFD);
    return decomposed.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }
}
