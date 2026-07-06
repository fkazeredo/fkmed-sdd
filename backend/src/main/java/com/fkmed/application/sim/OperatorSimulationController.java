package com.fkmed.application.sim;

import com.fkmed.infra.web.HttpRequestMetadata;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The operator-simulation REST family (SPEC-0018 tele slice, ADR-0017/DL-0021): the flag-gated
 * {@code /api/sim/**} endpoints that drive the professional-side telemedicine transitions and
 * clinical-document issuance the POC has no back office for. The whole controller is
 * {@code @ConditionalOnProperty(app.sim.enabled)} — so the routes are ABSENT (404) unless a dev/e2e
 * profile opts in (BR1) — and every action first asserts the internal OPERATOR_SIM role ({@link
 * OperatorSimAccess}, 403 for beneficiaries — BR2) and is audited with the operator as author
 * (BR3). The owning modules' state machines are respected (invalid → 409 — BR4). Tagged {@code
 * operator-simulation} in the OpenAPI snapshot (BR7).
 */
@RestController
@RequestMapping("/api/sim")
@ConditionalOnProperty(prefix = "app.sim", name = "enabled", havingValue = "true")
@Tag(name = "operator-simulation")
@RequiredArgsConstructor
public class OperatorSimulationController {

  private final SimService sim;
  private final OperatorSimAccess operatorAccess;

  /** Starts attending the next queued session (SPEC-0018 BR5, SPEC-0010 BR8). */
  @PostMapping("/tele/sessions/next/start")
  SimTeleSessionResult startNext(@Valid @RequestBody StartNextTeleRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.startNextTele(
        request.professionalName(), request.crm(), operator, HttpRequestMetadata.current());
  }

  /** Closes a session, atomically issuing its documents (SPEC-0018 BR5, SPEC-0010 BR9/BR10). */
  @PostMapping("/tele/sessions/{id}/close")
  SimTeleSessionResult close(
      @PathVariable UUID id, @Valid @RequestBody CloseTeleSessionRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.closeTele(
        id,
        request.professionalName(),
        request.crm(),
        request.guidance(),
        request.documentsOrEmpty(),
        operator,
        HttpRequestMetadata.current());
  }

  /** Issues an operator-authored clinical document for a beneficiary (SPEC-0018 BR5). */
  @PostMapping("/documents")
  SimIssuedDocumentResponse issueDocument(
      @Valid @RequestBody IssueOperatorDocumentRequest request) {
    UUID operator = operatorAccess.requireOperator();
    UUID documentId =
        sim.issueDocument(
            request.beneficiaryId(),
            request.professionalName(),
            request.crm(),
            request.spec(),
            operator,
            HttpRequestMetadata.current());
    return new SimIssuedDocumentResponse(documentId);
  }

  /** Opens a new guide as {@code EM_ANALISE} (SPEC-0018 BR5, SPEC-0012). */
  @PostMapping("/guides")
  @ResponseStatus(HttpStatus.CREATED)
  SimGuideResult createGuide(@Valid @RequestBody SimCreateGuideRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.createGuide(
        request.beneficiaryId(),
        request.type(),
        request.requestingProvider(),
        request.items(),
        operator,
        HttpRequestMetadata.current());
  }

  /** Authorizes every item of a guide (SPEC-0018 BR5). */
  @PostMapping("/guides/{id}/authorize")
  SimGuideResult authorizeGuide(
      @PathVariable UUID id, @Valid @RequestBody SimAuthorizeGuideRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.authorizeGuide(
        id, request.password(), request.validUntil(), operator, HttpRequestMetadata.current());
  }

  /** Applies a per-item authorization decision to a guide (SPEC-0018 BR5, SPEC-0012 BR6). */
  @PostMapping("/guides/{id}/partially-authorize")
  SimGuideResult partiallyAuthorizeGuide(
      @PathVariable UUID id, @Valid @RequestBody SimPartiallyAuthorizeGuideRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.partiallyAuthorizeGuide(
        id,
        request.password(),
        request.validUntil(),
        request.asMap(),
        operator,
        HttpRequestMetadata.current());
  }

  /** Denies every item of a guide (SPEC-0018 BR5). */
  @PostMapping("/guides/{id}/deny")
  SimGuideResult denyGuide(@PathVariable UUID id, @Valid @RequestBody SimDenyGuideRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.denyGuide(id, request.reason(), operator, HttpRequestMetadata.current());
  }

  /** Cancels a guide (SPEC-0018 BR5). */
  @PostMapping("/guides/{id}/cancel")
  SimGuideResult cancelGuide(@PathVariable UUID id) {
    UUID operator = operatorAccess.requireOperator();
    return sim.cancelGuide(id, operator, HttpRequestMetadata.current());
  }

  /** Marks a guide executed (SPEC-0018 BR5). */
  @PostMapping("/guides/{id}/mark-executed")
  SimGuideResult markGuideExecuted(@PathVariable UUID id) {
    UUID operator = operatorAccess.requireOperator();
    return sim.markGuideExecuted(id, operator, HttpRequestMetadata.current());
  }
}
