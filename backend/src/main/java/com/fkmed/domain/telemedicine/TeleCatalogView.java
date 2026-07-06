package com.fkmed.domain.telemedicine;

import java.util.List;

/**
 * The telemedicine triage catalog (SPEC-0010 GET /api/tele/catalog): the symptom registry plus the
 * current teleattendance term the beneficiary must accept.
 */
public record TeleCatalogView(List<SymptomOption> symptoms, TeleTermView term) {

  /**
   * A selectable triage symptom: registry code + label, plus {@code emergency} — whether picking it
   * raises the 24h-ER alert (SPEC-0010 BR3), the flag the FE's emergency banner reads.
   */
  public record SymptomOption(String code, String name, boolean emergency) {}

  /** The current term's version and body. */
  public record TeleTermView(String version, String body) {}
}
