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

  /** Issues a new OPEN invoice for a contract titular (SPEC-0013 §Operator-sim). */
  @PostMapping("/finance/invoices")
  @ResponseStatus(HttpStatus.CREATED)
  SimInvoiceResult createInvoice(@Valid @RequestBody SimCreateInvoiceRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.issueInvoice(
        request.titularBeneficiaryId(),
        request.competenciaDate(),
        request.dueDate(),
        request.amount(),
        request.digitableLine(),
        request.pixCode(),
        operator,
        HttpRequestMetadata.current());
  }

  /** Records the payment of an invoice, idempotently (SPEC-0018 BR6). */
  @PostMapping("/finance/invoices/{id}/pay")
  void payInvoice(@PathVariable UUID id) {
    UUID operator = operatorAccess.requireOperator();
    sim.payInvoice(id, operator, HttpRequestMetadata.current());
  }

  /** Records a copay charge for a family member's usage (SPEC-0013 §Operator-sim). */
  @PostMapping("/finance/copay")
  @ResponseStatus(HttpStatus.CREATED)
  SimCopayResult createCopay(@Valid @RequestBody SimCreateCopayRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.recordCopay(
        request.entryDate(),
        request.procedure(),
        request.provider(),
        request.beneficiaryId(),
        request.amount(),
        operator,
        HttpRequestMetadata.current());
  }

  /** Approves a reimbursement after engine calculation (SPEC-0018 BR5). */
  @PostMapping("/reimbursements/{id}/approve")
  SimReimbursementResult approveReimbursement(@PathVariable UUID id) {
    UUID operator = operatorAccess.requireOperator();
    return sim.approveReimbursement(id, operator, HttpRequestMetadata.current());
  }

  /** Denies a reimbursement with a visible reason (SPEC-0016 BR9, SPEC-0018 BR5). */
  @PostMapping("/reimbursements/{id}/deny")
  SimReimbursementResult denyReimbursement(
      @PathVariable UUID id, @Valid @RequestBody SimDenyReimbursementRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.denyReimbursement(id, request.reason(), operator, HttpRequestMetadata.current());
  }

  /** Opens a documentation pendency (SPEC-0016 BR6, SPEC-0018 BR5). */
  @PostMapping("/reimbursements/{id}/pendency")
  SimReimbursementResult openReimbursementPendency(
      @PathVariable UUID id, @Valid @RequestBody SimOpenReimbursementPendencyRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.openReimbursementPendency(
        id, request.description(), operator, HttpRequestMetadata.current());
  }

  /** Executes reimbursement payment, idempotently for already-paid requests (SPEC-0018 BR6). */
  @PostMapping("/reimbursements/{id}/pay")
  SimReimbursementResult payReimbursement(
      @PathVariable UUID id, @Valid @RequestBody SimPayReimbursementRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.payReimbursement(
        id, request.success(), request.failureReason(), operator, HttpRequestMetadata.current());
  }

  /** Concludes an analyzed reimbursement preview (SPEC-0017, SPEC-0018 BR5). */
  @PostMapping("/reimbursement-previews/{id}/conclude")
  SimPreviewResult concludePreview(
      @PathVariable UUID id, @Valid @RequestBody SimConcludePreviewRequest request) {
    UUID operator = operatorAccess.requireOperator();
    return sim.concludePreview(
        id, request.estimatedValue(), operator, HttpRequestMetadata.current());
  }
}
