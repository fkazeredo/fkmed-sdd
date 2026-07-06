package com.fkmed.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fkmed.domain.network.SpecialtyValidator;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.BeneficiaryNotAccessibleException;
import com.fkmed.domain.plan.BeneficiaryRole;
import com.fkmed.domain.plan.ProtocolGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * SPEC-0009 booking/cancel/reschedule orchestration — branch coverage for the mutation gate (the
 * transactional paths that {@code AppointmentConcurrencyIT}/{@code AppointmentApiIT} exercise end
 * to end, driven here as fast unit branches against mocked repositories, mirroring {@code
 * IdentityServiceTest}).
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
  private static final String CARD = "001234567";
  private static final UUID AUTHOR = UUID.fromString("a0000000-0000-4000-8000-000000000001");
  private static final UUID BENEFICIARY = UUID.fromString("b0000000-0000-4000-8000-000000000002");
  private static final UUID UNIT = UUID.fromString("c0000000-0000-4000-8000-000000000003");
  private static final UUID AGENDA = UUID.fromString("d0000000-0000-4000-8000-000000000004");
  private static final UUID SLOT = UUID.fromString("e0000000-0000-4000-8000-000000000005");
  private static final LocalDate DATE = LocalDate.parse("2026-07-08");
  private static final LocalTime TIME = LocalTime.of(9, 0);
  private static final LocalDateTime SLOT_LDT = LocalDateTime.of(DATE, TIME);
  private static final Instant SCHEDULED_AT = Instant.parse("2026-07-08T09:00:00Z");
  private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31}; // %PDF-1

  @Mock private CareUnitRepository careUnits;
  @Mock private UnitAgendaRepository agendas;
  @Mock private ScheduleSlotRepository slots;
  @Mock private ExamTypeRepository examTypes;
  @Mock private AppointmentRepository appointments;
  @Mock private AppointmentAttachmentRepository attachments;
  @Mock private SpecialtyValidator specialties;
  @Mock private BeneficiaryAccess beneficiaryAccess;
  @Mock private ProtocolGenerator protocolGenerator;
  @Mock private ApplicationEventPublisher events;

  private AppointmentService service;

  @BeforeEach
  void setUp() {
    service =
        new AppointmentService(
            careUnits,
            agendas,
            slots,
            examTypes,
            appointments,
            attachments,
            specialties,
            beneficiaryAccess,
            protocolGenerator,
            events,
            new SimpleMeterRegistry(),
            CLOCK);
  }

  // ---- book: consultation ----------------------------------------------------------------------

  @Test
  void book_consultation_occupiesSeat_allocatesProtocol_andPublishesConfirmed() {
    accessibleTitular();
    when(specialties.isValid("CARDIOLOGIA")).thenReturn(true);
    ScheduleSlot slot = slot(2, 0);
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", slot);
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(protocolGenerator.next("AG")).thenReturn("AG-20260708-0001");

    BookingConfirmation result = service.book(consultationCommand("CARDIOLOGIA"), null, null);

    assertThat(result.protocol()).isEqualTo("AG-20260708-0001");
    assertThat(result.status()).isEqualTo(AppointmentStatus.AGENDADO);
    assertThat(slot.getOccupied()).isEqualTo(1);
    verify(slots).saveAndFlush(slot);
    verify(appointments).save(any(Appointment.class));
    verify(attachments, never()).save(any());

    ArgumentCaptor<AppointmentConfirmed> event =
        ArgumentCaptor.forClass(AppointmentConfirmed.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().beneficiaryId()).isEqualTo(BENEFICIARY);
    assertThat(event.getValue().protocol()).isEqualTo("AG-20260708-0001");
    assertThat(event.getValue().specialtyOrExamCode()).isEqualTo("CARDIOLOGIA");
    assertThat(event.getValue().scheduledAt()).isEqualTo(SCHEDULED_AT);
  }

  @Test
  void book_consultation_withUnknownSpecialty_isRejected() {
    accessibleTitular();
    when(specialties.isValid("NOPE")).thenReturn(false);

    assertThatExceptionOfType(AppointmentOutsideHorizonException.class)
        .isThrownBy(() -> service.book(consultationCommand("NOPE"), null, null));
    verify(appointments, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void book_outOfScopeBeneficiary_isRejected_beforeTouchingSlots() {
    when(beneficiaryAccess.requireAccessible(CARD, BENEFICIARY))
        .thenThrow(new BeneficiaryNotAccessibleException());

    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(() -> service.book(consultationCommand("CARDIOLOGIA"), null, null));
    verify(slots, never()).saveAndFlush(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void book_whenSlotDoesNotExist_isOutsideHorizon() {
    accessibleTitular();
    when(specialties.isValid("CARDIOLOGIA")).thenReturn(true);
    when(agendas.findByUnitIdAndScopeTypeAndScopeCode(
            UNIT, AppointmentType.CONSULTATION, "CARDIOLOGIA"))
        .thenReturn(Optional.of(agenda(AppointmentType.CONSULTATION, "CARDIOLOGIA")));
    when(slots.findByAgendaIdAndSlotDateAndSlotTime(AGENDA, DATE, TIME))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(AppointmentOutsideHorizonException.class)
        .isThrownBy(() -> service.book(consultationCommand("CARDIOLOGIA"), null, null));
  }

  @Test
  void book_whenBeneficiaryAlreadyHasThatInstant_isTimeConflict() {
    accessibleTitular();
    when(specialties.isValid("CARDIOLOGIA")).thenReturn(true);
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", slot(1, 0));
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(
            eq(BENEFICIARY), eq(SCHEDULED_AT), any()))
        .thenReturn(Optional.of(existing()));

    assertThatExceptionOfType(AppointmentTimeConflictException.class)
        .isThrownBy(() -> service.book(consultationCommand("CARDIOLOGIA"), null, null));
    verify(slots, never()).saveAndFlush(any());
  }

  @Test
  void book_whenSlotIsFull_failsFast_asSlotUnavailable() {
    accessibleTitular();
    when(specialties.isValid("CARDIOLOGIA")).thenReturn(true);
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", slot(1, 1)); // full
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(SlotUnavailableException.class)
        .isThrownBy(() -> service.book(consultationCommand("CARDIOLOGIA"), null, null));
    verify(appointments, never()).save(any());
  }

  @Test
  void book_whenTheSeatRaceIsLost_atFlush_isSlotUnavailable() {
    accessibleTitular();
    when(specialties.isValid("CARDIOLOGIA")).thenReturn(true);
    ScheduleSlot slot = slot(1, 0);
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", slot);
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(slots.saveAndFlush(slot)).thenThrow(new OptimisticLockingFailureException("race"));

    assertThatExceptionOfType(SlotUnavailableException.class)
        .isThrownBy(() -> service.book(consultationCommand("CARDIOLOGIA"), null, null));
    verify(protocolGenerator, never()).next(anyString());
    verify(events, never()).publishEvent(any());
  }

  // ---- book: exam ------------------------------------------------------------------------------

  @Test
  void book_exam_validatesTheMedicalOrder_savesTheAttachment_andConfirms() {
    accessibleTitular();
    when(examTypes.existsById("HEMOGRAMA")).thenReturn(true);
    ScheduleSlot slot = slot(1, 0);
    stubBookableSlot(AppointmentType.EXAM, "HEMOGRAMA", slot);
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(protocolGenerator.next("AG")).thenReturn("AG-20260708-0002");

    BookingConfirmation result = service.book(examCommand("HEMOGRAMA"), PDF, "order.pdf");

    assertThat(result.protocol()).isEqualTo("AG-20260708-0002");
    verify(attachments).save(any(AppointmentAttachment.class));
    verify(events).publishEvent(any(AppointmentConfirmed.class));
  }

  @Test
  void book_exam_withUnknownExamCode_isRejected() {
    accessibleTitular();
    when(examTypes.existsById("NOPE")).thenReturn(false);

    assertThatExceptionOfType(AppointmentOutsideHorizonException.class)
        .isThrownBy(() -> service.book(examCommand("NOPE"), PDF, "order.pdf"));
  }

  @Test
  void book_exam_withoutTheMedicalOrder_isRejected() {
    accessibleTitular();
    when(examTypes.existsById("HEMOGRAMA")).thenReturn(true);
    stubBookableSlot(AppointmentType.EXAM, "HEMOGRAMA", slot(1, 0));
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatExceptionOfType(MedicalOrderRequiredException.class)
        .isThrownBy(() -> service.book(examCommand("HEMOGRAMA"), null, null));
    verify(appointments, never()).save(any());
  }

  // ---- cancel ----------------------------------------------------------------------------------

  @Test
  void cancel_releasesTheSeat_setsCancelled_andPublishesCancelled() {
    Appointment appt = upcoming();
    when(appointments.findById(appt.getId())).thenReturn(Optional.of(appt));
    accessibleContains(appt.getBeneficiaryId());
    ScheduleSlot slot = slot(2, 1);
    when(slots.findById(appt.getSlotId())).thenReturn(Optional.of(slot));

    BookingConfirmation result = service.cancel(CARD, AUTHOR, appt.getId(), "mudança de planos");

    assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELADO);
    assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CANCELADO);
    assertThat(slot.getOccupied()).isZero();
    verify(slots).save(slot);
    verify(events).publishEvent(any(AppointmentCancelled.class));
  }

  @Test
  void cancel_unknownAppointment_isNotFound() {
    UUID id = UUID.randomUUID();
    when(appointments.findById(id)).thenReturn(Optional.empty());

    assertThatExceptionOfType(AppointmentNotFoundException.class)
        .isThrownBy(() -> service.cancel(CARD, AUTHOR, id, null));
    verify(events, never()).publishEvent(any());
  }

  @Test
  void cancel_outOfScopeAppointment_isNotFound_neverRevealingExistence() {
    Appointment appt = upcoming();
    when(appointments.findById(appt.getId())).thenReturn(Optional.of(appt));
    when(beneficiaryAccess.accessibleFor(CARD)).thenReturn(List.of()); // no accessible beneficiary

    assertThatExceptionOfType(AppointmentNotFoundException.class)
        .isThrownBy(() -> service.cancel(CARD, AUTHOR, appt.getId(), null));
  }

  @Test
  void cancel_afterStartTime_isTooLate() {
    Appointment past = past();
    when(appointments.findById(past.getId())).thenReturn(Optional.of(past));
    accessibleContains(past.getBeneficiaryId());

    assertThatExceptionOfType(AppointmentTooLateException.class)
        .isThrownBy(() -> service.cancel(CARD, AUTHOR, past.getId(), null));
    verify(events, never()).publishEvent(any());
  }

  // ---- reschedule ------------------------------------------------------------------------------

  @Test
  void reschedule_occupiesNewSeat_releasesOld_keepsProtocol_andPublishesRescheduled() {
    Appointment appt = upcoming();
    when(appointments.findById(appt.getId())).thenReturn(Optional.of(appt));
    accessibleContains(appt.getBeneficiaryId());
    ScheduleSlot target = slot(2, 0);
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", target);
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(
            eq(BENEFICIARY), eq(SCHEDULED_AT), any()))
        .thenReturn(Optional.empty());
    ScheduleSlot old = slot(2, 1);
    when(slots.findById(appt.getSlotId())).thenReturn(Optional.of(old));

    BookingConfirmation result = service.reschedule(CARD, AUTHOR, appt.getId(), SLOT_LDT);

    assertThat(result.status()).isEqualTo(AppointmentStatus.REAGENDADO);
    assertThat(result.protocol()).isEqualTo(appt.getProtocol());
    assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.REAGENDADO);
    assertThat(target.getOccupied()).isEqualTo(1);
    assertThat(old.getOccupied()).isZero();
    verify(events).publishEvent(any(AppointmentRescheduled.class));
  }

  @Test
  void reschedule_afterStartTime_isTooLate() {
    Appointment past = past();
    when(appointments.findById(past.getId())).thenReturn(Optional.of(past));
    accessibleContains(past.getBeneficiaryId());

    assertThatExceptionOfType(AppointmentTooLateException.class)
        .isThrownBy(() -> service.reschedule(CARD, AUTHOR, past.getId(), SLOT_LDT));
  }

  @Test
  void reschedule_ontoAConflictingInstant_isRejected() {
    Appointment appt = upcoming();
    when(appointments.findById(appt.getId())).thenReturn(Optional.of(appt));
    accessibleContains(appt.getBeneficiaryId());
    stubBookableSlot(AppointmentType.CONSULTATION, "CARDIOLOGIA", slot(2, 0));
    Appointment other = existing();
    when(appointments.findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(
            eq(BENEFICIARY), eq(SCHEDULED_AT), any()))
        .thenReturn(Optional.of(other));

    assertThatExceptionOfType(AppointmentTimeConflictException.class)
        .isThrownBy(() -> service.reschedule(CARD, AUTHOR, appt.getId(), SLOT_LDT));
  }

  // ---- reads -----------------------------------------------------------------------------------

  @Test
  void availability_keepsOnlyBookableSlots_withRemainingCapacity() {
    when(agendas.findByUnitIdAndScopeTypeAndScopeCode(
            UNIT, AppointmentType.CONSULTATION, "CARDIOLOGIA"))
        .thenReturn(Optional.of(agenda(AppointmentType.CONSULTATION, "CARDIOLOGIA")));
    ScheduleSlot bookable = slotAt(DATE, LocalTime.of(9, 0), 5, 2);
    ScheduleSlot full = slotAt(DATE, LocalTime.of(10, 0), 5, 5);
    ScheduleSlot pastDay = slotAt(LocalDate.parse("2026-07-05"), LocalTime.of(9, 0), 5, 0);
    when(slots.findByAgendaIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
            eq(AGENDA), any(), any()))
        .thenReturn(List.of(bookable, full, pastDay));

    AvailabilityResponse response =
        service.availability(UNIT, AppointmentType.CONSULTATION, "CARDIOLOGIA");

    assertThat(response.days()).hasSize(1);
    assertThat(response.days().get(0).date()).isEqualTo(DATE);
    assertThat(response.days().get(0).slots())
        .extracting(AvailabilityResponse.Slot::slot)
        .containsExactly("2026-07-08T09:00", "2026-07-08T10:00");
    assertThat(response.days().get(0).slots().get(0).remaining()).isEqualTo(3);
    assertThat(response.days().get(0).slots().get(0).available()).isTrue();
    assertThat(response.days().get(0).slots().get(1).remaining()).isZero();
    assertThat(response.days().get(0).slots().get(1).available()).isFalse();
  }

  @Test
  void availability_forAnUnknownScope_isEmpty() {
    when(agendas.findByUnitIdAndScopeTypeAndScopeCode(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThat(service.availability(UNIT, AppointmentType.CONSULTATION, "GHOST").days()).isEmpty();
  }

  @Test
  void list_splitsUpcomingFromHistory_andResolvesNames() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(
            List.of(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR)));
    Appointment upcoming = upcoming();
    Appointment past = past();
    when(appointments.findByBeneficiaryIdInOrderByScheduledAtDesc(Set.of(BENEFICIARY)))
        .thenReturn(List.of(upcoming, past));

    AppointmentListResponse response = service.list(CARD, null, false);

    assertThat(response.upcoming())
        .extracting(AppointmentView::protocol)
        .containsExactly(upcoming.getProtocol());
    assertThat(response.history())
        .extracting(AppointmentView::status)
        .containsExactly(AppointmentStatus.REALIZADO);
    assertThat(response.upcoming().get(0).beneficiaryName()).isEqualTo("Maria");
  }

  @Test
  void list_withAFilterOutsideScope_returnsEmpty() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(
            List.of(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR)));

    AppointmentListResponse response = service.list(CARD, UUID.randomUUID(), false);

    assertThat(response.upcoming()).isEmpty();
    assertThat(response.history()).isEmpty();
    verify(appointments, never()).findByBeneficiaryIdInOrderByScheduledAtDesc(any());
  }

  @Test
  void list_withTelemedicineOnly_keepsOnlyTelemedicineModality() {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(
            List.of(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR)));
    when(appointments.findByBeneficiaryIdInOrderByScheduledAtDesc(Set.of(BENEFICIARY)))
        .thenReturn(List.of(teleUpcoming(), upcoming()));

    AppointmentListResponse response = service.list(CARD, null, true);

    assertThat(response.upcoming())
        .extracting(AppointmentView::modality)
        .containsExactly(AppointmentModality.TELEMEDICINA);
  }

  @Test
  void telemedicineUnitId_resolvesTheSeededVirtualUnit() {
    CareUnit unit = org.mockito.Mockito.mock(CareUnit.class);
    when(unit.getId()).thenReturn(UNIT);
    when(careUnits.findFirstByVirtualTrue()).thenReturn(Optional.of(unit));

    assertThat(service.telemedicineUnitId()).isEqualTo(UNIT);
  }

  @Test
  void telemedicineUnitId_withNoVirtualUnit_throwsIllegalState() {
    when(careUnits.findFirstByVirtualTrue()).thenReturn(Optional.empty());

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> service.telemedicineUnitId());
  }

  @Test
  void unitsServing_mapsToViews() {
    when(careUnits.findServingScope(AppointmentType.CONSULTATION, "CARDIOLOGIA"))
        .thenReturn(List.of(careUnit("FKMed Unidade Centro")));

    List<CareUnitView> views = service.unitsServing(AppointmentType.CONSULTATION, "CARDIOLOGIA");

    assertThat(views).extracting(CareUnitView::name).containsExactly("FKMed Unidade Centro");
  }

  @Test
  void examCatalog_mapsToViews() {
    when(examTypes.findAll(any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(examType("HEMOGRAMA", "Hemograma")));

    assertThat(service.examCatalog()).extracting(ExamTypeView::code).containsExactly("HEMOGRAMA");
  }

  // ---- fixtures --------------------------------------------------------------------------------

  private void accessibleTitular() {
    when(beneficiaryAccess.requireAccessible(CARD, BENEFICIARY))
        .thenReturn(new AccessibleBeneficiary(BENEFICIARY, "Maria", BeneficiaryRole.TITULAR));
  }

  private void accessibleContains(UUID beneficiaryId) {
    when(beneficiaryAccess.accessibleFor(CARD))
        .thenReturn(
            List.of(new AccessibleBeneficiary(beneficiaryId, "Maria", BeneficiaryRole.TITULAR)));
  }

  private void stubBookableSlot(AppointmentType type, String code, ScheduleSlot slot) {
    lenient()
        .when(agendas.findByUnitIdAndScopeTypeAndScopeCode(UNIT, type, code))
        .thenReturn(Optional.of(agenda(type, code)));
    lenient()
        .when(slots.findByAgendaIdAndSlotDateAndSlotTime(AGENDA, DATE, TIME))
        .thenReturn(Optional.of(slot));
  }

  private BookAppointmentCommand consultationCommand(String specialtyCode) {
    return new BookAppointmentCommand(
        CARD,
        AUTHOR,
        BENEFICIARY,
        AppointmentType.CONSULTATION,
        specialtyCode,
        null,
        UNIT,
        SLOT_LDT);
  }

  private BookAppointmentCommand examCommand(String examCode) {
    return new BookAppointmentCommand(
        CARD, AUTHOR, BENEFICIARY, AppointmentType.EXAM, null, examCode, UNIT, SLOT_LDT);
  }

  private static Appointment upcoming() {
    return Appointment.consultation(
        BENEFICIARY,
        "CARDIOLOGIA",
        AppointmentModality.PRESENCIAL,
        UNIT,
        SLOT,
        SCHEDULED_AT,
        "AG-20260708-0009",
        AUTHOR,
        CLOCK.instant());
  }

  private static Appointment teleUpcoming() {
    return Appointment.consultation(
        BENEFICIARY,
        "CLINICA_MEDICA",
        AppointmentModality.TELEMEDICINA,
        UNIT,
        SLOT,
        SCHEDULED_AT,
        "AG-20260708-0010",
        AUTHOR,
        CLOCK.instant());
  }

  private static Appointment past() {
    return Appointment.consultation(
        BENEFICIARY,
        "CARDIOLOGIA",
        AppointmentModality.PRESENCIAL,
        UNIT,
        SLOT,
        Instant.parse("2026-07-05T09:00:00Z"),
        "AG-20260705-0001",
        AUTHOR,
        Instant.parse("2026-07-01T09:00:00Z"));
  }

  private static Appointment existing() {
    return Appointment.consultation(
        BENEFICIARY,
        "DERMATOLOGIA",
        AppointmentModality.PRESENCIAL,
        UNIT,
        UUID.randomUUID(),
        SCHEDULED_AT,
        "AG-20260708-0003",
        AUTHOR,
        CLOCK.instant());
  }

  private static ScheduleSlot slot(int capacity, int occupied) {
    return slotAt(DATE, TIME, capacity, occupied);
  }

  private static ScheduleSlot slotAt(LocalDate date, LocalTime time, int capacity, int occupied) {
    ScheduleSlot slot = new ScheduleSlot();
    set(slot, "id", SLOT);
    set(slot, "agendaId", AGENDA);
    set(slot, "slotDate", date);
    set(slot, "slotTime", time);
    set(slot, "capacity", capacity);
    set(slot, "occupied", occupied);
    return slot;
  }

  private static UnitAgenda agenda(AppointmentType type, String code) {
    UnitAgenda agenda = new UnitAgenda();
    set(agenda, "id", AGENDA);
    set(agenda, "unitId", UNIT);
    set(agenda, "scopeType", type);
    set(agenda, "scopeCode", code);
    return agenda;
  }

  private static CareUnit careUnit(String name) {
    CareUnit unit = new CareUnit();
    set(unit, "id", UNIT);
    set(unit, "name", name);
    return unit;
  }

  private static ExamType examType(String code, String name) {
    ExamType exam = new ExamType();
    set(exam, "code", code);
    set(exam, "name", name);
    return exam;
  }

  private static void set(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
