package com.fkmed.domain.guides;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

/**
 * An authorization guide (SPEC-0012): the immutable request header (number, type, requesting
 * provider, request date) plus the mutable authorization outcome (password, validity, denial
 * reason) and its {@link GuideItem} children. The status is guarded by an optimistic {@link
 * Version} lock; transitions go only through the state machine ({@link
 * GuideStatus#canTransitionTo}) and — for authorize/partially-authorize/deny — DERIVE from the
 * items' resulting statuses (BR6), never set directly. Created and moved ONLY through {@link
 * GuideService}, the seam the operator simulation (SPEC-0018) drives; no beneficiary write path
 * exists.
 */
@Entity
@Table(name = "guide")
@Getter
public class Guide {

  @Id private UUID id;

  @Column(nullable = false, unique = true)
  private String number;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GuideType type;

  @Column(name = "beneficiary_id", nullable = false)
  private UUID beneficiaryId;

  @Column(name = "requesting_provider", nullable = false)
  private String requestingProvider;

  @Column(name = "requested_at", nullable = false)
  private LocalDate requestedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GuideStatus status;

  @Column(name = "auth_password")
  private String authPassword;

  @Column(name = "auth_valid_until")
  private LocalDate authValidUntil;

  @Column(name = "denial_reason")
  private String denialReason;

  @OneToMany(
      mappedBy = "guide",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("tussCode asc")
  private List<GuideItem> items = new ArrayList<>();

  @Version
  @Column(nullable = false)
  private long version;

  /** JPA only. */
  protected Guide() {}

  /** Opens a new guide as {@link GuideStatus#EM_ANALISE} (SPEC-0018 BR5). */
  static Guide open(
      String number,
      GuideType type,
      UUID beneficiaryId,
      String requestingProvider,
      LocalDate requestedAt,
      List<GuideItemInput> itemInputs) {
    if (itemInputs == null || itemInputs.isEmpty()) {
      throw new IllegalArgumentException("a guide requires at least one item");
    }
    Guide guide = new Guide();
    guide.id = UUID.randomUUID();
    guide.number = number;
    guide.type = type;
    guide.beneficiaryId = beneficiaryId;
    guide.requestingProvider = requestingProvider;
    guide.requestedAt = requestedAt;
    guide.status = GuideStatus.EM_ANALISE;
    guide.items = itemInputs.stream().map(input -> GuideItem.of(guide, input)).toList();
    return guide;
  }

  /**
   * Authorizes every item (BR5/BR6): all items become {@link GuideItemStatus#AUTORIZADO}, the guide
   * moves to {@link GuideStatus#AUTORIZADA}.
   *
   * @throws IllegalStateException when the guide is not {@link GuideStatus#EM_ANALISE}.
   */
  void authorize(String password, LocalDate validUntil) {
    requireAnalysis();
    items.forEach(item -> item.applyStatus(GuideItemStatus.AUTORIZADO));
    this.authPassword = password;
    this.authValidUntil = validUntil;
    this.status = deriveStatusFromItems();
  }

  /**
   * Applies a per-item authorization decision (BR5/BR6): the resulting overall status DERIVES from
   * the items — a full mix of authorized/denied yields {@link GuideStatus#PARCIALMENTE_AUTORIZADA}.
   *
   * @throws IllegalStateException when the guide is not {@link GuideStatus#EM_ANALISE}.
   */
  void partiallyAuthorize(
      String password, LocalDate validUntil, Map<String, GuideItemStatus> itemStatuses) {
    requireAnalysis();
    for (GuideItem item : items) {
      GuideItemStatus decision = itemStatuses.get(item.getTussCode());
      if (decision != null) {
        item.applyStatus(decision);
      }
    }
    this.authPassword = password;
    this.authValidUntil = validUntil;
    this.status = deriveStatusFromItems();
  }

  /**
   * Denies every item (BR5/BR6): all items become {@link GuideItemStatus#NEGADO}, the guide moves
   * to {@link GuideStatus#NEGADA} with {@code reason}.
   *
   * @throws IllegalStateException when the guide is not {@link GuideStatus#EM_ANALISE}.
   */
  void deny(String reason) {
    requireAnalysis();
    items.forEach(item -> item.applyStatus(GuideItemStatus.NEGADO));
    this.denialReason = reason;
    this.status = deriveStatusFromItems();
  }

  /**
   * Cancels the guide (BR6): allowed from {@link GuideStatus#EM_ANALISE}, {@link
   * GuideStatus#AUTORIZADA} or {@link GuideStatus#PARCIALMENTE_AUTORIZADA}.
   *
   * @throws IllegalStateException when cancellation is not allowed from the current status.
   */
  void cancel() {
    transitionTo(GuideStatus.CANCELADA);
  }

  /**
   * Marks the guide executed (BR6): allowed only from {@link GuideStatus#AUTORIZADA} or {@link
   * GuideStatus#PARCIALMENTE_AUTORIZADA}.
   *
   * @throws IllegalStateException when the guide is not authorized (fully or partially).
   */
  void markExecuted() {
    transitionTo(GuideStatus.EXECUTADA);
  }

  /** Whether the stamped authorization validity has passed as of {@code today}. */
  public boolean authExpired(LocalDate today) {
    return authValidUntil != null && authValidUntil.isBefore(today);
  }

  private void requireAnalysis() {
    if (status != GuideStatus.EM_ANALISE) {
      throw new IllegalStateException(
          "invalid guide transition: " + status + " does not accept an authorization decision");
    }
  }

  private void transitionTo(GuideStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new IllegalStateException("invalid guide transition " + status + " -> " + target);
    }
    this.status = target;
  }

  /**
   * Derives the overall status from the items' resulting statuses (BR6): all {@link
   * GuideItemStatus#AUTORIZADO} yields {@link GuideStatus#AUTORIZADA}, all {@link
   * GuideItemStatus#NEGADO} yields {@link GuideStatus#NEGADA}, anything else (a mix, or an item
   * still under analysis) yields {@link GuideStatus#PARCIALMENTE_AUTORIZADA}.
   */
  GuideStatus deriveStatusFromItems() {
    boolean allAuthorized =
        items.stream().allMatch(item -> item.getStatus() == GuideItemStatus.AUTORIZADO);
    if (allAuthorized) {
      return GuideStatus.AUTORIZADA;
    }
    boolean allDenied = items.stream().allMatch(item -> item.getStatus() == GuideItemStatus.NEGADO);
    if (allDenied) {
      return GuideStatus.NEGADA;
    }
    return GuideStatus.PARCIALMENTE_AUTORIZADA;
  }
}
