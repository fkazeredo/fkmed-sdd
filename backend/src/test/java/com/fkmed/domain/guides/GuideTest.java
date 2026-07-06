package com.fkmed.domain.guides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0012 BR6: the guide's status DERIVES from its items' resulting statuses on every
 * authorization decision — never set directly — and transitions are guarded by the state machine.
 */
class GuideTest {

  private static final UUID BENEFICIARY = UUID.randomUUID();
  private static final LocalDate REQUESTED = LocalDate.of(2026, 6, 1);

  @Test
  void open_startsEmAnalise_withItemsUnderAnalysis() {
    Guide guide = twoItemGuide();

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.EM_ANALISE);
    assertThat(guide.getItems()).hasSize(2);
    assertThat(guide.getItems()).allMatch(item -> item.getStatus() == GuideItemStatus.EM_ANALISE);
  }

  @Test
  void open_withNoItems_throws() {
    assertThatThrownBy(
            () ->
                Guide.open(
                    "GD-1", GuideType.CONSULTA, BENEFICIARY, "Dr. Fulano", REQUESTED, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void authorize_setsEveryItemAuthorized_andDerivesAutorizada() {
    Guide guide = twoItemGuide();

    guide.authorize("AUT-1", REQUESTED.plusDays(30));

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.AUTORIZADA);
    assertThat(guide.getAuthPassword()).isEqualTo("AUT-1");
    assertThat(guide.getAuthValidUntil()).isEqualTo(REQUESTED.plusDays(30));
    assertThat(guide.getItems()).allMatch(item -> item.getStatus() == GuideItemStatus.AUTORIZADO);
  }

  @Test
  void authorize_whenNotEmAnalise_throws() {
    Guide guide = twoItemGuide();
    guide.authorize("AUT-1", REQUESTED.plusDays(30));

    assertThatThrownBy(() -> guide.authorize("AUT-2", REQUESTED.plusDays(60)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void partiallyAuthorize_mixedDecision_derivesParciallyAutorizada() {
    Guide guide = twoItemGuide();
    Map<String, GuideItemStatus> decisions =
        Map.of("T1", GuideItemStatus.AUTORIZADO, "T2", GuideItemStatus.NEGADO);

    guide.partiallyAuthorize("AUT-9", REQUESTED.plusDays(10), decisions);

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.PARCIALMENTE_AUTORIZADA);
    assertThat(itemStatus(guide, "T1")).isEqualTo(GuideItemStatus.AUTORIZADO);
    assertThat(itemStatus(guide, "T2")).isEqualTo(GuideItemStatus.NEGADO);
  }

  @Test
  void partiallyAuthorize_whenEveryItemEndsUpAuthorized_derivesAutorizada() {
    Guide guide = twoItemGuide();
    Map<String, GuideItemStatus> decisions =
        Map.of("T1", GuideItemStatus.AUTORIZADO, "T2", GuideItemStatus.AUTORIZADO);

    guide.partiallyAuthorize("AUT-9", REQUESTED.plusDays(10), decisions);

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.AUTORIZADA);
  }

  @Test
  void partiallyAuthorize_whenEveryItemEndsUpDenied_derivesNegada() {
    Guide guide = twoItemGuide();
    Map<String, GuideItemStatus> decisions =
        Map.of("T1", GuideItemStatus.NEGADO, "T2", GuideItemStatus.NEGADO);

    guide.partiallyAuthorize("AUT-9", REQUESTED.plusDays(10), decisions);

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.NEGADA);
  }

  @Test
  void deny_setsEveryItemDenied_andRecordsTheReason() {
    Guide guide = twoItemGuide();

    guide.deny("Documentação insuficiente");

    assertThat(guide.getStatus()).isEqualTo(GuideStatus.NEGADA);
    assertThat(guide.getDenialReason()).isEqualTo("Documentação insuficiente");
    assertThat(guide.getItems()).allMatch(item -> item.getStatus() == GuideItemStatus.NEGADO);
  }

  @Test
  void cancel_isAllowedFromAnalysisOrAuthorized_butNotFromAFinalState() {
    Guide fromAnalysis = twoItemGuide();
    fromAnalysis.cancel();
    assertThat(fromAnalysis.getStatus()).isEqualTo(GuideStatus.CANCELADA);

    Guide fromAuthorized = twoItemGuide();
    fromAuthorized.authorize("AUT-1", REQUESTED.plusDays(30));
    fromAuthorized.cancel();
    assertThat(fromAuthorized.getStatus()).isEqualTo(GuideStatus.CANCELADA);

    Guide denied = twoItemGuide();
    denied.deny("motivo");
    assertThatThrownBy(denied::cancel).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void markExecuted_isAllowedOnlyFromAuthorizedStates() {
    Guide notYetAuthorized = twoItemGuide();
    assertThatThrownBy(notYetAuthorized::markExecuted).isInstanceOf(IllegalStateException.class);

    Guide authorized = twoItemGuide();
    authorized.authorize("AUT-1", REQUESTED.plusDays(30));
    authorized.markExecuted();
    assertThat(authorized.getStatus()).isEqualTo(GuideStatus.EXECUTADA);
  }

  @Test
  void authExpired_derivesFromValidUntilAgainstToday() {
    Guide guide = twoItemGuide();
    guide.authorize("AUT-1", LocalDate.of(2026, 7, 1));

    assertThat(guide.authExpired(LocalDate.of(2026, 6, 30))).isFalse();
    assertThat(guide.authExpired(LocalDate.of(2026, 7, 1))).isFalse();
    assertThat(guide.authExpired(LocalDate.of(2026, 7, 2))).isTrue();
  }

  @Test
  void authExpired_whenNeverAuthorized_isFalse() {
    Guide guide = twoItemGuide();
    assertThat(guide.authExpired(LocalDate.of(2026, 12, 31))).isFalse();
  }

  private static Guide twoItemGuide() {
    return Guide.open(
        "GD-TEST",
        GuideType.SP_SADT,
        BENEFICIARY,
        "Laboratório Central",
        REQUESTED,
        List.of(new GuideItemInput("T1", "Item um", 1), new GuideItemInput("T2", "Item dois", 2)));
  }

  private static GuideItemStatus itemStatus(Guide guide, String tussCode) {
    return guide.getItems().stream()
        .filter(item -> item.getTussCode().equals(tussCode))
        .findFirst()
        .orElseThrow()
        .getStatus();
  }
}
