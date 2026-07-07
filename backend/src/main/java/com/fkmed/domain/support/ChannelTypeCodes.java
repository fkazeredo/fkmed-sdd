package com.fkmed.domain.support;

import java.util.Set;

/**
 * The fixed set of channel-card types (SPEC-0014 BR1: Central 24h · WhatsApp · Ouvidoria · ANS).
 * Not a registry table and not a business enum: the four types are fixed by the product's contact
 * surface (no runtime editing, no per-code wired branching), so per DECISIONS-BASELINE §0019 they
 * are validated as stable {@code String} codes through this {@code *Codes} constants holder.
 */
public final class ChannelTypeCodes {

  /** Central de Atendimento 24h (capitals + other-localities numbers). */
  public static final String CENTRAL = "CENTRAL";

  /** WhatsApp oficial. */
  public static final String WHATSAPP = "WHATSAPP";

  /** Ouvidoria. */
  public static final String OUVIDORIA = "OUVIDORIA";

  /** ANS (informative agency number). */
  public static final String ANS = "ANS";

  private static final Set<String> ALL = Set.of(CENTRAL, WHATSAPP, OUVIDORIA, ANS);

  private ChannelTypeCodes() {}

  /** Whether {@code code} is one of the four fixed channel types. */
  public static boolean isValid(String code) {
    return code != null && ALL.contains(code);
  }
}
