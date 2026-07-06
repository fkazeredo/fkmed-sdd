package com.fkmed.domain.support;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Case/accent-insensitive substring matching (SPEC-0014 BR5: the FAQ search must match {@code
 * "reembolso"} against {@code "Reembolso"} regardless of case or diacritics). Pure, no
 * infrastructure — the FAQ seed is small enough (a dozen-odd rows) that filtering in Java is
 * simpler than an indexed database search extension (Rule Zero); revisit with a {@code
 * pg_trgm}/{@code unaccent} index if the catalog grows enough to matter. Mirrors {@code
 * domain.network.NormalizedText} — duplicated rather than shared across modules (DECISIONS-BASELINE
 * §0016 module boundaries; the two utilities are trivial and evolve independently).
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
