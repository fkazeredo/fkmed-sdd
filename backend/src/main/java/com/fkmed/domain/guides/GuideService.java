package com.fkmed.domain.guides;

import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.ProtocolGenerator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade of the guide aggregate (SPEC-0012): the beneficiary-facing
 * list/detail reads (BR2/BR5) and the operator-driven transitions (SPEC-0018 BR5) — create,
 * authorize, partially authorize, deny, cancel, mark executed. Family scope is enforced through
 * {@link BeneficiaryAccess} (SPEC-0003 BR3) exactly like {@code AppointmentService#book}; guide
 * numbers reuse the shared {@link ProtocolGenerator} (prefix {@code GD}). Every transition
 * publishes {@link GuideStatusChanged} inside the transaction (BR8); the notification listener
 * (wired at integration) delivers it AFTER_COMMIT. Mirrors {@code
 * com.fkmed.domain.telemedicine.TeleService}'s single-service shape (both reads and the
 * operator-sim seam in one class — Rule Zero).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuideService {

  /**
   * SPEC-0018 BR5 guide-number prefix ({@link ProtocolGenerator}, mirroring {@code AG-}/{@code
   * RE-}).
   */
  private static final String NUMBER_PREFIX = "GD";

  private final GuideRepository guides;
  private final BeneficiaryAccess beneficiaryAccess;
  private final ProtocolGenerator protocolGenerator;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * The beneficiary's guides, most-recent-first, optionally narrowed by {@code status}/{@code
   * period} (BR2). Family scope is enforced (SPEC-0003 BR3); an out-of-scope beneficiary never
   * reveals whether it exists.
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   */
  @Transactional(readOnly = true)
  public List<GuideListItem> list(
      String callerCard, UUID beneficiaryId, GuideStatus status, GuidePeriod period) {
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);
    LocalDate from = period == null ? null : period.from(LocalDate.now(clock));
    return guides.findByBeneficiaryIdOrderByRequestedAtDesc(beneficiaryId).stream()
        .filter(guide -> status == null || guide.getStatus() == status)
        .filter(guide -> from == null || !guide.getRequestedAt().isBefore(from))
        .map(GuideService::toListItem)
        .toList();
  }

  /**
   * The type-specific detail of {@code guideId} within the caller's family scope (BR5).
   *
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     the caller's scope.
   * @throws GuideNotFoundException when the guide is unknown or does not belong to {@code
   *     beneficiaryId} (existence never revealed).
   */
  @Transactional(readOnly = true)
  public GuideDetail detail(String callerCard, UUID beneficiaryId, UUID guideId) {
    beneficiaryAccess.requireAccessible(callerCard, beneficiaryId);
    Guide guide =
        guides
            .findById(guideId)
            .filter(candidate -> candidate.getBeneficiaryId().equals(beneficiaryId))
            .orElseThrow(GuideNotFoundException::new);
    return toDetail(guide, LocalDate.now(clock));
  }

  /** Opens a new guide as {@link GuideStatus#EM_ANALISE} (SPEC-0018 BR5). No event published. */
  @Transactional
  public Guide createGuide(
      GuideType type, UUID beneficiaryId, String requestingProvider, List<GuideItemInput> items) {
    String number = protocolGenerator.next(NUMBER_PREFIX);
    Guide guide =
        Guide.open(number, type, beneficiaryId, requestingProvider, LocalDate.now(clock), items);
    guides.save(guide);
    log.info("guide {} opened for a beneficiary", number);
    return guide;
  }

  /**
   * Authorizes every item (SPEC-0018 BR5).
   *
   * @throws GuideNotFoundException when unknown; {@link IllegalStateException} when not {@link
   *     GuideStatus#EM_ANALISE} (translated to {@code 409} by the sim seam).
   */
  @Transactional
  public Guide authorize(UUID guideId, String password, LocalDate validUntil) {
    Guide guide = requireGuide(guideId);
    guide.authorize(password, validUntil);
    guides.save(guide);
    publishTransition(guide);
    return guide;
  }

  /**
   * Applies a per-item authorization decision (SPEC-0018 BR5); the overall status derives from the
   * items (BR6).
   *
   * @throws GuideNotFoundException when unknown; {@link IllegalStateException} when not {@link
   *     GuideStatus#EM_ANALISE}.
   */
  @Transactional
  public Guide partiallyAuthorize(
      UUID guideId,
      String password,
      LocalDate validUntil,
      Map<String, GuideItemStatus> itemStatuses) {
    Guide guide = requireGuide(guideId);
    guide.partiallyAuthorize(password, validUntil, itemStatuses);
    guides.save(guide);
    publishTransition(guide);
    return guide;
  }

  /**
   * Denies every item with {@code reason} (SPEC-0018 BR5).
   *
   * @throws GuideNotFoundException when unknown; {@link IllegalStateException} when not {@link
   *     GuideStatus#EM_ANALISE}.
   */
  @Transactional
  public Guide deny(UUID guideId, String reason) {
    Guide guide = requireGuide(guideId);
    guide.deny(reason);
    guides.save(guide);
    publishTransition(guide);
    return guide;
  }

  /**
   * Cancels the guide (SPEC-0018 BR5).
   *
   * @throws GuideNotFoundException when unknown; {@link IllegalStateException} when cancellation is
   *     not allowed from the current status.
   */
  @Transactional
  public Guide cancel(UUID guideId) {
    Guide guide = requireGuide(guideId);
    guide.cancel();
    guides.save(guide);
    publishTransition(guide);
    return guide;
  }

  /**
   * Marks the guide executed (SPEC-0018 BR5).
   *
   * @throws GuideNotFoundException when unknown; {@link IllegalStateException} when the guide is
   *     not authorized (fully or partially).
   */
  @Transactional
  public Guide markExecuted(UUID guideId) {
    Guide guide = requireGuide(guideId);
    guide.markExecuted();
    guides.save(guide);
    publishTransition(guide);
    return guide;
  }

  private Guide requireGuide(UUID guideId) {
    return guides.findById(guideId).orElseThrow(GuideNotFoundException::new);
  }

  private void publishTransition(Guide guide) {
    log.info("guide {} moved to {}", guide.getNumber(), guide.getStatus());
    events.publishEvent(
        new GuideStatusChanged(
            guide.getId(),
            guide.getBeneficiaryId(),
            guide.getNumber(),
            guide.getStatus(),
            guide.getDenialReason()));
  }

  private static GuideListItem toListItem(Guide guide) {
    return new GuideListItem(
        guide.getId(),
        guide.getNumber(),
        guide.getType(),
        guide.getRequestingProvider(),
        guide.getRequestedAt(),
        guide.getStatus());
  }

  private static GuideDetail toDetail(Guide guide, LocalDate today) {
    boolean authorized =
        guide.getStatus() == GuideStatus.AUTORIZADA
            || guide.getStatus() == GuideStatus.PARCIALMENTE_AUTORIZADA;
    boolean denied = guide.getStatus() == GuideStatus.NEGADA;
    return new GuideDetail(
        guide.getId(),
        guide.getNumber(),
        guide.getType(),
        guide.getRequestingProvider(),
        guide.getRequestedAt(),
        guide.getStatus(),
        authorized ? guide.getAuthPassword() : null,
        authorized ? guide.getAuthValidUntil() : null,
        guide.authExpired(today),
        denied ? guide.getDenialReason() : null,
        guide.getItems().stream()
            .map(
                item ->
                    new GuideDetail.ItemView(
                        item.getTussCode(),
                        item.getDescription(),
                        item.getQuantity(),
                        item.getStatus()))
            .toList());
  }
}
