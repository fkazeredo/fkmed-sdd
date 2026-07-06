package com.fkmed.domain.support;

/**
 * Support channel type (SPEC-0014 BR1).
 *
 * <p>Kept as an enum rather than registry data (invariant 7 / DECISIONS-BASELINE §0019): it is a
 * fixed, code-branched rendering classification — exactly the four channel kinds the frontend
 * renders with a different action each (tap-to-call for {@code CENTRAL}/{@code OUVIDORIA}/{@code
 * ANS}, a WhatsApp deep link for {@code WHATSAPP}) — not an operator-growing business vocabulary.
 */
public enum ChannelType {
  CENTRAL,
  WHATSAPP,
  OUVIDORIA,
  ANS
}
