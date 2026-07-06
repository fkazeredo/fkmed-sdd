package com.fkmed.domain.telemedicine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The telemedicine state machine (SPEC-0010 BR11 + the BR8 no-show edge): the allowed transitions,
 * the active/final classification and that finals are dead ends.
 */
class TeleSessionStateTest {

  @Test
  void emFila_mayReachTheTurnOrBeAbandoned_butNotBeClosed() {
    assertThat(TeleSessionState.EM_FILA.canTransitionTo(TeleSessionState.EM_ATENDIMENTO)).isTrue();
    assertThat(TeleSessionState.EM_FILA.canTransitionTo(TeleSessionState.ABANDONADA)).isTrue();
    assertThat(TeleSessionState.EM_FILA.canTransitionTo(TeleSessionState.ENCERRADA)).isFalse();
    assertThat(TeleSessionState.EM_FILA.canTransitionTo(TeleSessionState.EM_FILA)).isFalse();
  }

  @Test
  void emAtendimento_mayBeClosedOrAbandoned_butNotReturnToQueue() {
    assertThat(TeleSessionState.EM_ATENDIMENTO.canTransitionTo(TeleSessionState.ENCERRADA))
        .isTrue();
    // The 5-minute no-show edge (BR8/AC3) that BR11's happy-path list omits.
    assertThat(TeleSessionState.EM_ATENDIMENTO.canTransitionTo(TeleSessionState.ABANDONADA))
        .isTrue();
    assertThat(TeleSessionState.EM_ATENDIMENTO.canTransitionTo(TeleSessionState.EM_FILA)).isFalse();
  }

  @Test
  void finalStates_areDeadEnds() {
    for (TeleSessionState target : TeleSessionState.values()) {
      assertThat(TeleSessionState.ENCERRADA.canTransitionTo(target)).isFalse();
      assertThat(TeleSessionState.ABANDONADA.canTransitionTo(target)).isFalse();
    }
  }

  @Test
  void activeAndFinal_partitionTheStates() {
    assertThat(TeleSessionState.EM_FILA.isActive()).isTrue();
    assertThat(TeleSessionState.EM_ATENDIMENTO.isActive()).isTrue();
    assertThat(TeleSessionState.ENCERRADA.isActive()).isFalse();
    assertThat(TeleSessionState.ABANDONADA.isActive()).isFalse();

    assertThat(TeleSessionState.EM_FILA.isFinal()).isFalse();
    assertThat(TeleSessionState.ENCERRADA.isFinal()).isTrue();
    assertThat(TeleSessionState.ABANDONADA.isFinal()).isTrue();
  }
}
