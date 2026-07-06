package com.fkmed.application.api;

import com.fkmed.domain.guides.GuideDetail;
import com.fkmed.domain.guides.GuideListItem;
import com.fkmed.domain.guides.GuidePeriod;
import com.fkmed.domain.guides.GuideService;
import com.fkmed.domain.guides.GuideStatus;
import com.fkmed.infra.security.UserContextProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guide endpoints (SPEC-0012 BR2/BR5): the filtered list and the type-specific detail. Read-only
 * for beneficiaries — family scope and the derivation/display rules all live in {@code
 * domain.guides.GuideService}; the caller's beneficiary card is resolved from the JWT, never
 * client-supplied. {@code beneficiaryId} is required (no "todos" aggregation — the frozen
 * contract): the active-beneficiary selector on the client always sends one.
 */
@RestController
@RequestMapping("/api/guides")
@RequiredArgsConstructor
public class GuideController {

  private final GuideService guides;
  private final UserContextProvider userContext;

  /** The Minha Guias list (BR2), most-recent-first, optionally filtered by status and period. */
  @GetMapping
  List<GuideListItem> list(
      @RequestParam UUID beneficiaryId,
      @RequestParam(required = false) GuideStatus status,
      @RequestParam(required = false) GuidePeriod period) {
    return guides.list(callerCard(), beneficiaryId, status, period);
  }

  /** The guide detail (BR5); 404 when unknown or out of the caller's family scope. */
  @GetMapping("/{id}")
  GuideDetail detail(@PathVariable UUID id, @RequestParam UUID beneficiaryId) {
    return guides.detail(callerCard(), beneficiaryId, id);
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }
}
