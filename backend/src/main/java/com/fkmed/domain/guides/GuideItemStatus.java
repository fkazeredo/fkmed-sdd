package com.fkmed.domain.guides;

/**
 * The lifecycle of a single guide item (SPEC-0012 BR6): under analysis, authorized or denied by the
 * operator. The guide's own {@link GuideStatus} derives from the aggregate of its items' statuses.
 *
 * <p>Kept as an enum under invariant 7 / DECISIONS-BASELINE §0019: it is a lifecycle state machine
 * whose values the guide's derivation logic enforces — not reference data.
 */
public enum GuideItemStatus {
  EM_ANALISE,
  AUTORIZADO,
  NEGADO
}
