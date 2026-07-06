package com.fkmed.application.sim;

import com.fkmed.domain.guides.GuideItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Body of {@code POST /api/sim/guides/{id}/partially-authorize} (SPEC-0018 BR5): a per-item
 * authorization decision; the guide's overall status derives from the resulting mix (BR6).
 */
public record SimPartiallyAuthorizeGuideRequest(
    @NotBlank String password,
    @NotNull LocalDate validUntil,
    @NotEmpty @Valid List<ItemStatus> itemStatuses) {

  /** One item's authorization decision, keyed by TUSS code. */
  public record ItemStatus(@NotBlank String tussCode, @NotNull GuideItemStatus status) {}

  /** The decisions as a {@code tussCode -> status} map, the shape {@code Guide} consumes. */
  public Map<String, GuideItemStatus> asMap() {
    return itemStatuses.stream()
        .collect(Collectors.toMap(ItemStatus::tussCode, ItemStatus::status));
  }
}
