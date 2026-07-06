package com.fkmed.application.sim;

import com.fkmed.domain.guides.GuideType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Body of {@code POST /api/sim/guides} (SPEC-0018 BR5): opens a new guide as {@code EM_ANALISE} for
 * a beneficiary, with at least one requested item.
 */
public record SimCreateGuideRequest(
    @NotNull UUID beneficiaryId,
    @NotNull GuideType type,
    @NotBlank String requestingProvider,
    @NotEmpty @Valid List<Item> items) {

  /** One requested TUSS item. */
  public record Item(@NotBlank String tussCode, @NotBlank String description, int quantity) {}
}
