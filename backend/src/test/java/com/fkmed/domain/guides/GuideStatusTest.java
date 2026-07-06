package com.fkmed.domain.guides;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The guide lifecycle state machine (SPEC-0012 BR6). */
class GuideStatusTest {

  @Test
  void emAnalise_mayMoveToAnyOutcomeOrBeCancelled() {
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.AUTORIZADA)).isTrue();
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.PARCIALMENTE_AUTORIZADA))
        .isTrue();
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.NEGADA)).isTrue();
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.CANCELADA)).isTrue();
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.EXECUTADA)).isFalse();
    assertThat(GuideStatus.EM_ANALISE.canTransitionTo(GuideStatus.EM_ANALISE)).isFalse();
  }

  @Test
  void authorized_mayBeExecutedOrCancelled_butNeverReturnToAnalysis() {
    for (GuideStatus authorized :
        new GuideStatus[] {GuideStatus.AUTORIZADA, GuideStatus.PARCIALMENTE_AUTORIZADA}) {
      assertThat(authorized.canTransitionTo(GuideStatus.EXECUTADA)).isTrue();
      assertThat(authorized.canTransitionTo(GuideStatus.CANCELADA)).isTrue();
      assertThat(authorized.canTransitionTo(GuideStatus.EM_ANALISE)).isFalse();
      assertThat(authorized.canTransitionTo(GuideStatus.NEGADA)).isFalse();
    }
  }

  @Test
  void finalStates_areDeadEnds() {
    for (GuideStatus target : GuideStatus.values()) {
      assertThat(GuideStatus.NEGADA.canTransitionTo(target)).isFalse();
      assertThat(GuideStatus.CANCELADA.canTransitionTo(target)).isFalse();
      assertThat(GuideStatus.EXECUTADA.canTransitionTo(target)).isFalse();
    }
  }

  @Test
  void isFinal_classifiesTerminalStatuses() {
    assertThat(GuideStatus.EM_ANALISE.isFinal()).isFalse();
    assertThat(GuideStatus.AUTORIZADA.isFinal()).isFalse();
    assertThat(GuideStatus.PARCIALMENTE_AUTORIZADA.isFinal()).isFalse();
    assertThat(GuideStatus.NEGADA.isFinal()).isTrue();
    assertThat(GuideStatus.CANCELADA.isFinal()).isTrue();
    assertThat(GuideStatus.EXECUTADA.isFinal()).isTrue();
  }
}
