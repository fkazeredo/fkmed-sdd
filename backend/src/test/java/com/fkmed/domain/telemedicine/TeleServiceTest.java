package com.fkmed.domain.telemedicine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.appointment.AppointmentService;
import com.fkmed.domain.appointment.TeleJoinTarget;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryRole;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * SPEC-0010 orchestration — fast branch coverage for the mutation gate (the paths the {@code
 * Tele*IT} suites exercise end to end, driven here against mocked repositories): the triage/term
 * validations, the single-active-session resume/race (BR7), the queue position/ETA and room view
 * (BR5/BR6), the scheduled-join window (BR14), the no-show sweep (BR8) and the professional seams.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeleServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String CARD = "001234567";
  private static final UUID BEN = UUID.fromString("b0000000-0000-4000-8000-000000000002");
  private static final UUID AUTHOR = UUID.fromString("a0000000-0000-4000-8000-000000000001");

  @Mock private SymptomRepository symptoms;
  @Mock private TeleTermRepository teleTerms;
  @Mock private TeleSessionRepository sessions;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private AppointmentService appointments;
  @Mock private com.fkmed.domain.clinicaldocs.ClinicalDocuments clinicalDocuments;
  @Mock private ApplicationEventPublisher events;
  @Mock private PlatformTransactionManager transactionManager;

  private TeleService service;

  @BeforeEach
  void setUp() {
    service =
        new TeleService(
            symptoms,
            teleTerms,
            sessions,
            beneficiaryAccess,
            appointments,
            clinicalDocuments,
            events,
            new SimpleMeterRegistry(),
            transactionManager,
            CLOCK);
    when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    TeleTerm currentTerm = term("1.0");
    when(teleTerms.findTopByOrderByPublishedAtDesc()).thenReturn(Optional.of(currentTerm));
  }

  // ---- enterQueue validations -----------------------------------------------------------------

  @Test
  void enter_withComplaintShorterThanTen_throwsComplaintInvalid() {
    accessible();
    assertThatExceptionOfType(TeleComplaintInvalidException.class)
        .isThrownBy(
            () -> service.enterQueue(command("too short", List.of("CEFALEIA"), "D1_3", "1.0")));
    verify(sessions, never()).saveAndFlush(any());
  }

  @Test
  void enter_withStaleTermVersion_throwsTermNotAccepted() {
    accessible();
    assertThatExceptionOfType(TeleTermNotAcceptedException.class)
        .isThrownBy(
            () ->
                service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "D1_3", "0.9")));
  }

  @Test
  void enter_withUnknownSymptomOrBadDuration_throwsTriageInvalid() {
    accessible();
    when(symptoms.existsById("CEFALEIA")).thenReturn(true);
    assertThatExceptionOfType(TeleTriageInvalidException.class)
        .isThrownBy(
            () ->
                service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "MESES", "1.0")));

    when(symptoms.existsById("NOPE")).thenReturn(false);
    assertThatExceptionOfType(TeleTriageInvalidException.class)
        .isThrownBy(
            () -> service.enterQueue(command(validComplaint(), List.of("NOPE"), "D1_3", "1.0")));
  }

  // ---- enterQueue create / resume / race ------------------------------------------------------

  @Test
  void enter_whenNoActiveSession_createsAtPositionOneAndEtaThree() {
    accessible();
    validTriage();
    when(activeWalkIn()).thenReturn(Optional.empty());
    when(sessions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(sessions.countByTypeAndStateAndQueueEnteredAtBefore(any(), any(), any())).thenReturn(0L);

    EnterQueueResult result =
        service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "D1_3", "1.0"));

    assertThat(result.resumed()).isFalse();
    assertThat(result.state()).isEqualTo("EM_FILA");
    assertThat(result.position()).isEqualTo(1);
    assertThat(result.etaMinutes()).isEqualTo(3);
  }

  @Test
  void enter_computesPositionAndEtaFromTheQueueAhead() {
    accessible();
    validTriage();
    when(activeWalkIn()).thenReturn(Optional.empty());
    when(sessions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(sessions.countByTypeAndStateAndQueueEnteredAtBefore(any(), any(), any())).thenReturn(3L);

    EnterQueueResult result =
        service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "D1_3", "1.0"));

    assertThat(result.position()).isEqualTo(4);
    assertThat(result.etaMinutes()).isEqualTo(12);
  }

  @Test
  void enter_whenAlreadyQueued_resumesWithoutCreating() {
    accessible();
    validTriage();
    TeleSession existing = walkIn();
    when(activeWalkIn()).thenReturn(Optional.of(existing));
    when(sessions.countByTypeAndStateAndQueueEnteredAtBefore(any(), any(), any())).thenReturn(0L);

    EnterQueueResult result =
        service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "D1_3", "1.0"));

    assertThat(result.resumed()).isTrue();
    verify(sessions, never()).saveAndFlush(any());
  }

  @Test
  void enter_whenTwoRaceOnTheIndex_theLoserResumesTheWinner() {
    accessible();
    validTriage();
    TeleSession winner = walkIn();
    when(activeWalkIn()).thenReturn(Optional.empty()).thenReturn(Optional.of(winner));
    when(sessions.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq"));
    when(sessions.countByTypeAndStateAndQueueEnteredAtBefore(any(), any(), any())).thenReturn(0L);

    EnterQueueResult result =
        service.enterQueue(command(validComplaint(), List.of("CEFALEIA"), "D1_3", "1.0"));

    assertThat(result.resumed()).isTrue();
  }

  // ---- current / leave ------------------------------------------------------------------------

  @Test
  void currentSessionFor_marksTheResponseWhenObservingTheRoom() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(List.of(new AccessibleBeneficiary(BEN, "Maria", BeneficiaryRole.TITULAR)));
    TeleSession attended = walkIn();
    attended.reachTurn("Dra. Ana", "CRM-RJ 1", NOW);
    when(sessions.findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(any(), any()))
        .thenReturn(Optional.of(attended));

    Optional<TeleCurrentSession> current = service.currentSessionFor(CARD);

    assertThat(current).isPresent();
    assertThat(current.get().view().professional().name()).isEqualTo("Dra. Ana");
    assertThat(current.get().view().room()).isNotNull();
    assertThat(attended.getStartedAt()).isEqualTo(NOW);
    verify(sessions).save(attended);
  }

  @Test
  void currentSessionFor_withoutAccessibleBeneficiaries_isEmpty() {
    when(beneficiaryAccess.accessibleFor(CARD)).thenReturn(List.of());
    assertThat(service.currentSessionFor(CARD)).isEmpty();
  }

  @Test
  void leave_withoutActiveSession_throwsSessionNotFound() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(List.of(new AccessibleBeneficiary(BEN, "Maria", BeneficiaryRole.TITULAR)));
    when(sessions.findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(any(), any()))
        .thenReturn(Optional.empty());
    assertThatExceptionOfType(TeleSessionNotFoundException.class)
        .isThrownBy(() -> service.leave(CARD));
  }

  @Test
  void leave_abandonsTheActiveSession() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(List.of(new AccessibleBeneficiary(BEN, "Maria", BeneficiaryRole.TITULAR)));
    TeleSession queued = walkIn();
    when(sessions.findFirstByBeneficiaryIdInAndStateInOrderByCreatedAtDesc(any(), any()))
        .thenReturn(Optional.of(queued));

    TeleSessionView view = service.leave(CARD);

    assertThat(view.state()).isEqualTo("ABANDONADA");
    verify(sessions).save(queued);
  }

  // ---- scheduled join window ------------------------------------------------------------------

  @Test
  void join_withinWindow_createsScheduledSession() {
    UUID appointmentId = UUID.randomUUID();
    when(appointments.teleJoinTarget(CARD, appointmentId))
        .thenReturn(new TeleJoinTarget(appointmentId, BEN, NOW, true, true, AUTHOR));
    when(sessions.findFirstByAppointmentIdAndStateIn(eq(appointmentId), any()))
        .thenReturn(Optional.empty());
    when(sessions.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

    TeleSessionView view = service.joinScheduled(CARD, appointmentId);
    assertThat(view.state()).isEqualTo("EM_FILA");
  }

  @Test
  void join_beforeWindowOpens_throwsJoinWindowClosed() {
    UUID appointmentId = UUID.randomUUID();
    when(appointments.teleJoinTarget(CARD, appointmentId))
        .thenReturn(
            new TeleJoinTarget(
                appointmentId, BEN, NOW.plus(Duration.ofMinutes(30)), true, true, AUTHOR));
    assertThatExceptionOfType(TeleJoinWindowClosedException.class)
        .isThrownBy(() -> service.joinScheduled(CARD, appointmentId));
  }

  @Test
  void join_afterWindowCloses_throwsJoinWindowClosed() {
    UUID appointmentId = UUID.randomUUID();
    when(appointments.teleJoinTarget(CARD, appointmentId))
        .thenReturn(
            new TeleJoinTarget(
                appointmentId, BEN, NOW.minus(Duration.ofMinutes(31)), true, true, AUTHOR));
    assertThatExceptionOfType(TeleJoinWindowClosedException.class)
        .isThrownBy(() -> service.joinScheduled(CARD, appointmentId));
  }

  @Test
  void join_forANonTeleOrInactiveAppointment_throwsSessionNotFound() {
    UUID appointmentId = UUID.randomUUID();
    when(appointments.teleJoinTarget(CARD, appointmentId))
        .thenReturn(new TeleJoinTarget(appointmentId, BEN, NOW, false, true, AUTHOR));
    assertThatExceptionOfType(TeleSessionNotFoundException.class)
        .isThrownBy(() -> service.joinScheduled(CARD, appointmentId));
  }

  // ---- professional seams + no-show + viewOf --------------------------------------------------

  @Test
  void reachTurn_movesToAttendingAndPublishesTurnReached() {
    TeleSession queued = walkIn();
    UUID sessionId = queued.getId();
    when(sessions.findById(sessionId)).thenReturn(Optional.of(queued));

    service.reachTurn(sessionId, "Dra. Ana", "CRM-RJ 1");

    assertThat(queued.getState()).isEqualTo(TeleSessionState.EM_ATENDIMENTO);
    ArgumentCaptor<TeleTurnReached> event = ArgumentCaptor.forClass(TeleTurnReached.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().sessionId()).isEqualTo(sessionId);
    assertThat(event.getValue().beneficiaryId()).isEqualTo(BEN);
  }

  @Test
  void close_endsTheSessionAndPublishesSessionClosed() {
    TeleSession attended = walkIn();
    attended.reachTurn("Dra. Ana", "CRM-RJ 1", NOW);
    when(sessions.findById(attended.getId())).thenReturn(Optional.of(attended));

    service.close(attended.getId(), new TeleClosureSummary("Dra. Ana", "CRM-RJ 1", "Repouso"));

    assertThat(attended.getState()).isEqualTo(TeleSessionState.ENCERRADA);
    ArgumentCaptor<TeleSessionClosed> event = ArgumentCaptor.forClass(TeleSessionClosed.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().guidance()).isEqualTo("Repouso");
  }

  @Test
  void expireNoShows_abandonsStaleAttendedSessions() {
    TeleSession attended = walkIn();
    attended.reachTurn("Dra. Ana", "CRM-RJ 1", NOW.minus(Duration.ofMinutes(6)));
    when(sessions.findByStateAndStartedAtIsNullAndCalledAtBefore(
            eq(TeleSessionState.EM_ATENDIMENTO), any()))
        .thenReturn(List.of(attended));

    int expired = service.expireNoShows();

    assertThat(expired).isEqualTo(1);
    assertThat(attended.getState()).isEqualTo(TeleSessionState.ABANDONADA);
    verify(sessions).save(attended);
  }

  @Test
  void viewOf_returnsTheView_forActiveAndFinalStates_andEmptyForUnknown() {
    TeleSession queued = walkIn();
    when(sessions.findById(queued.getId())).thenReturn(Optional.of(queued));
    when(sessions.countByTypeAndStateAndQueueEnteredAtBefore(any(), any(), any())).thenReturn(0L);
    assertThat(service.viewOf(queued.getId())).isPresent();

    // Wave 2: viewOf now yields the final view too, so the SSE can push the terminal state once
    // before completing (BR9 closure summary / abandoned notice) rather than dropping it silently.
    queued.leave(NOW);
    assertThat(service.viewOf(queued.getId())).map(TeleSessionView::state).contains("ABANDONADA");

    UUID unknown = UUID.randomUUID();
    when(sessions.findById(unknown)).thenReturn(Optional.empty());
    assertThat(service.viewOf(unknown)).isEmpty();
  }

  @Test
  void viewOf_closedSession_buildsClosureRoomSummaryWithDurationAndDocuments() {
    TeleSession attended = walkIn();
    attended.reachTurn("Dra. Ana", "CRM-RJ 1", NOW);
    attended.markResponded(NOW);
    attended.close("Dra. Ana", "CRM-RJ 1", "Repouso", NOW.plus(Duration.ofMinutes(15)));
    when(sessions.findById(attended.getId())).thenReturn(Optional.of(attended));
    UUID docId = UUID.randomUUID();
    when(clinicalDocuments.issuedForSession(attended.getId()))
        .thenReturn(
            List.of(
                new com.fkmed.domain.clinicaldocs.IssuedDocumentSummary(docId, "PRESCRIPTION")));

    TeleSessionView view = service.viewOf(attended.getId()).orElseThrow();

    assertThat(view.state()).isEqualTo("ENCERRADA");
    assertThat(view.room().durationMinutes()).isEqualTo(15);
    assertThat(view.room().guidance()).isEqualTo("Repouso");
    assertThat(view.room().documents())
        .extracting(TeleSessionView.IssuedDocument::type)
        .containsExactly("PRESCRIPTION");
  }

  @Test
  void reachNextTurn_reachesTheOldestQueuedSession() {
    TeleSession queued = walkIn();
    when(sessions.findFirstByTypeAndStateOrderByQueueEnteredAtAsc(
            TeleSessionType.WALK_IN, TeleSessionState.EM_FILA))
        .thenReturn(Optional.of(queued));
    when(sessions.findById(queued.getId())).thenReturn(Optional.of(queued));

    UUID id = service.reachNextTurn("Dra. Ana", "CRM-RJ 1");

    assertThat(id).isEqualTo(queued.getId());
    assertThat(queued.getState()).isEqualTo(TeleSessionState.EM_ATENDIMENTO);
  }

  @Test
  void reachNextTurn_withEmptyQueue_throwsSessionNotFound() {
    when(sessions.findFirstByTypeAndStateOrderByQueueEnteredAtAsc(any(), any()))
        .thenReturn(Optional.empty());
    assertThatExceptionOfType(TeleSessionNotFoundException.class)
        .isThrownBy(() -> service.reachNextTurn("Dra. Ana", "CRM-RJ 1"));
  }

  // ---- helpers --------------------------------------------------------------------------------

  private void accessible() {
    when(beneficiaryAccess.requireAccessible(CARD, BEN))
        .thenReturn(new AccessibleBeneficiary(BEN, "Maria", BeneficiaryRole.TITULAR));
  }

  private void validTriage() {
    when(symptoms.existsById("CEFALEIA")).thenReturn(true);
  }

  private Optional<TeleSession> activeWalkIn() {
    return sessions.findFirstByBeneficiaryIdAndTypeAndStateInOrderByCreatedAtDesc(
        eq(BEN), eq(TeleSessionType.WALK_IN), any());
  }

  private TeleSession walkIn() {
    return TeleSession.walkIn(
        BEN, validComplaint(), List.of("CEFALEIA"), null, "D1_3", "1.0", AUTHOR, NOW);
  }

  private static String validComplaint() {
    return "Dor de cabeça há dois dias";
  }

  private EnterQueueCommand command(
      String complaint, List<String> symptomCodes, String duration, String term) {
    return new EnterQueueCommand(CARD, AUTHOR, BEN, complaint, symptomCodes, null, duration, term);
  }

  private TeleTerm term(String version) {
    TeleTerm t = org.mockito.Mockito.mock(TeleTerm.class);
    when(t.getVersion()).thenReturn(version);
    when(t.getBody()).thenReturn("corpo do termo");
    return t;
  }
}
