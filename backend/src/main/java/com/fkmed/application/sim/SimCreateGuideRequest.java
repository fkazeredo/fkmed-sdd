package com.fkmed.application.sim;

import com.fkmed.domain.guides.GuideType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
    @NotEmpty @Valid List<GuideItemRequest> items) {

  /**
   * One requested TUSS item. Named {@code GuideItemRequest} (not {@code Item}) so its OpenAPI
   * schema does not collide with the pre-existing {@code ClinicalDocumentListResponse.Item} —
   * springdoc keys schemas by simple class name, and a collision silently overwrites one.
   */
  public record GuideItemRequest(
      @NotBlank String tussCode, @NotBlank String description, @Positive int quantity) {}
}
