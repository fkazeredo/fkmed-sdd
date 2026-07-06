package com.fkmed.domain.guides;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The guide detail (SPEC-0012 BR5): the common header plus its items, the authorization password
 * and validity (only when {@link #status} is {@link GuideStatus#AUTORIZADA} or {@link
 * GuideStatus#PARCIALMENTE_AUTORIZADA}), the derived {@link #authExpired} badge and the denial
 * reason (only when {@link #status} is {@link GuideStatus#NEGADA}).
 */
public record GuideDetail(
    UUID id,
    String number,
    GuideType type,
    String requestingProvider,
    LocalDate requestedAt,
    GuideStatus status,
    String authPassword,
    LocalDate authValidUntil,
    boolean authExpired,
    String denialReason,
    List<ItemView> items) {

  /** One item of the guide (BR5): TUSS code, description, quantity and its own status. */
  public record ItemView(
      String tussCode, String description, int quantity, GuideItemStatus status) {}
}
