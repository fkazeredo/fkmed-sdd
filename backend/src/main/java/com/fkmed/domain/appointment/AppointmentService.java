package com.fkmed.domain.appointment;

import com.fkmed.domain.network.SpecialtyValidator;
import com.fkmed.domain.plan.AccessibleBeneficiary;
import com.fkmed.domain.plan.BeneficiaryAccess;
import com.fkmed.domain.plan.ProtocolGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade of the appointment module (SPEC-0009). Owns the booking,
 * cancellation and rescheduling use cases against real slot capacity, plus the availability, unit
 * and Meus Agendamentos reads. Slot capacity is guarded fail-fast by {@link ScheduleSlot}'s
 * optimistic {@code @Version} (BR6/AC3, ADR-0012); protocols come from the shared {@link
 * ProtocolGenerator} (prefix {@code AG-}, BR7); family scope is enforced through {@link
 * BeneficiaryAccess} (BR1) and specialties are validated against the network registry ({@link
 * SpecialtyValidator}). Confirm/cancel/reschedule publish their domain events inside the
 * transaction; the notification listener (wired at integration) delivers them AFTER_COMMIT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

  /** SPEC-0003 BR9 protocol prefix for appointments (agendamento). */
  private static final String PROTOCOL_PREFIX = "AG";

  private static final Set<AppointmentStatus> ACTIVE_STATUSES =
      Set.of(AppointmentStatus.AGENDADO, AppointmentStatus.REAGENDADO);

  private final CareUnitRepository careUnits;
  private final UnitAgendaRepository agendas;
  private final ScheduleSlotRepository slots;
  private final ExamTypeRepository examTypes;
  private final AppointmentRepository appointments;
  private final AppointmentAttachmentRepository attachments;
  private final SpecialtyValidator specialties;
  private final BeneficiaryAccess beneficiaryAccess;
  private final ProtocolGenerator protocolGenerator;
  private final ApplicationEventPublisher events;
  private final MeterRegistry metrics;
  private final Clock clock;

  /** The units serving a specialty or exam, alphabetical by name (BR3/BR4). */
  @Transactional(readOnly = true)
  public List<CareUnitView> unitsServing(AppointmentType type, String code) {
    return careUnits.findServingScope(type, code).stream().map(CareUnitView::from).toList();
  }

  /**
   * The id of the seeded virtual Telemedicina unit (SPEC-0010 BR14, DL-0018): scheduled
   * teleconsultation booking/availability resolve it server-side from the {@code telemedicine=true}
   * scope, so the caller never sends a {@code unitId}. A booking against it is recorded as {@code
   * TELEMEDICINA} (see {@link #modalityOf}).
   *
   * @throws IllegalStateException when no virtual unit is seeded — a deployment/seed defect.
   */
  @Transactional(readOnly = true)
  public UUID telemedicineUnitId() {
    return careUnits
        .findFirstByVirtualTrue()
        .map(CareUnit::getId)
        .orElseThrow(() -> new IllegalStateException("no virtual Telemedicina unit is seeded"));
  }

  /** The exam catalog, alphabetical by name — the exam wizard's first step (BR4). */
  @Transactional(readOnly = true)
  public List<ExamTypeView> examCatalog() {
    return examTypes.findAll(org.springframework.data.domain.Sort.by("name")).stream()
        .map(ExamTypeView::from)
        .toList();
  }

  /**
   * The availability calendar for a (unit, scope): the days within today..+30 with slots respecting
   * the 2-hour antecedence, each carrying its remaining capacity (BR5, DL-0013). Full slots are
   * returned with {@code remaining == 0} so the client renders them unavailable rather than hiding
   * them. An unknown unit/scope yields an empty calendar.
   */
  @Transactional(readOnly = true)
  public AvailabilityResponse availability(UUID unitId, AppointmentType type, String code) {
    Instant now = clock.instant();
    ZoneId zone = clock.getZone();
    List<AvailabilityResponse.Day> days =
        agendas
            .findByUnitIdAndScopeTypeAndScopeCode(unitId, type, code)
            .map(
                agenda ->
                    slots
                        .findByAgendaIdAndSlotDateBetweenOrderBySlotDateAscSlotTimeAsc(
                            agenda.getId(),
                            BookingHorizon.today(now, zone),
                            BookingHorizon.horizonEnd(now, zone))
                        .stream()
                        .filter(
                            s ->
                                BookingHorizon.isBookable(
                                    s.getSlotDate(), s.getSlotTime(), now, zone))
                        .collect(Collectors.groupingBy(ScheduleSlot::getSlotDate)))
            .orElseGet(Map::of)
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                entry ->
                    new AvailabilityResponse.Day(
                        entry.getKey(),
                        entry.getValue().stream()
                            .sorted(Comparator.comparing(ScheduleSlot::getSlotTime))
                            .map(
                                s ->
                                    new AvailabilityResponse.Slot(
                                        LocalDateTime.of(s.getSlotDate(), s.getSlotTime())
                                            .toString(),
                                        s.remaining(),
                                        s.remaining() > 0))
                            .toList()))
            .toList();
    return new AvailabilityResponse(unitId, type, code, days);
  }

  /**
   * Confirms a booking as {@code AGENDADO}: validates the family scope (BR1) and the specialty/exam
   * (BR3/BR4), resolves the slot within the horizon (BR5), rejects a same-time conflict (BR8),
   * requires and validates the medical order for exams (BR4), occupies the seat fail-fast (BR6),
   * allocates the protocol (BR7) and publishes {@link AppointmentConfirmed}.
   *
   * @param medicalOrder the exam attachment bytes (ignored for consultations), {@code null}/empty
   *     when absent.
   * @throws com.fkmed.domain.plan.BeneficiaryNotAccessibleException when the beneficiary is out of
   *     scope; {@link AppointmentOutsideHorizonException}, {@link
   *     AppointmentTimeConflictException}, {@link MedicalOrderRequiredException}, {@link
   *     MedicalOrderInvalidException} and {@link SlotUnavailableException} per the rules above.
   */
  @Transactional
  public BookingConfirmation book(
      BookAppointmentCommand command, byte[] medicalOrder, String fileName) {
    AccessibleBeneficiary beneficiary =
        beneficiaryAccess.requireAccessible(command.callerCard(), command.beneficiaryId());
    Instant now = clock.instant();
    ZoneId zone = clock.getZone();

    String scopeCode = validatedScopeCode(command);
    ScheduleSlot slot =
        resolveBookableSlot(command.unitId(), command.type(), scopeCode, command.slot(), now, zone);
    Instant scheduledAt =
        BookingHorizon.instantOf(command.slot().toLocalDate(), command.slot().toLocalTime(), zone);
    requireNoConflict(beneficiary.beneficiaryId(), scheduledAt, null);

    MedicalOrderContent order =
        command.type() == AppointmentType.EXAM ? MedicalOrderContent.of(medicalOrder) : null;

    occupySeat(slot);
    String protocol = protocolGenerator.next(PROTOCOL_PREFIX);
    Appointment appointment =
        command.type() == AppointmentType.CONSULTATION
            ? Appointment.consultation(
                beneficiary.beneficiaryId(),
                scopeCode,
                modalityOf(command.unitId()),
                command.unitId(),
                slot.getId(),
                scheduledAt,
                protocol,
                command.authorAccountId(),
                now)
            : Appointment.exam(
                beneficiary.beneficiaryId(),
                scopeCode,
                command.unitId(),
                slot.getId(),
                scheduledAt,
                protocol,
                command.authorAccountId(),
                now);
    appointments.save(appointment);
    if (order != null) {
      attachments.save(AppointmentAttachment.of(appointment.getId(), order, fileName, now));
    }

    metrics.counter("appointment.confirmed").increment();
    log.info(
        "appointment {} confirmed for beneficiary {}",
        protocol,
        mask(appointment.getBeneficiaryId()));
    events.publishEvent(
        new AppointmentConfirmed(
            appointment.getId(),
            appointment.getBeneficiaryId(),
            protocol,
            appointment.getType().name(),
            appointment.scopeCode(),
            appointment.getUnitId(),
            scheduledAt,
            appointment.getCreatedBy()));
    return new BookingConfirmation(protocol, AppointmentStatus.AGENDADO);
  }

  /**
   * Cancels an upcoming appointment (BR9): releases its seat, sets {@code CANCELADO} keeping it in
   * history and publishes {@link AppointmentCancelled}. Blocked once the start time passes.
   *
   * @throws AppointmentNotFoundException when unknown or out of scope (existence never revealed).
   * @throws AppointmentTooLateException when the appointment is no longer an upcoming commitment.
   */
  @Transactional
  public BookingConfirmation cancel(
      String callerCard, UUID authorAccountId, UUID appointmentId, String reason) {
    Appointment appointment = requireAccessibleAppointment(callerCard, appointmentId);
    Instant now = clock.instant();
    appointment.cancel(reason, now);
    releaseSeat(appointment.getSlotId());
    appointments.save(appointment);

    metrics.counter("appointment.cancelled").increment();
    log.info(
        "appointment {} cancelled for beneficiary {}",
        appointment.getProtocol(),
        mask(appointment.getBeneficiaryId()));
    events.publishEvent(cancelledEvent(appointment, authorAccountId));
    return new BookingConfirmation(appointment.getProtocol(), AppointmentStatus.CANCELADO);
  }

  /**
   * Reschedules an upcoming appointment to a new slot (BR10): keeps beneficiary, specialty/exam,
   * unit and protocol, occupies the new seat fail-fast, releases the old one, sets {@code
   * REAGENDADO} and publishes {@link AppointmentRescheduled}. Only the date/time changes.
   *
   * @throws AppointmentNotFoundException when unknown or out of scope.
   * @throws AppointmentTooLateException when the appointment is no longer an upcoming commitment.
   * @throws AppointmentOutsideHorizonException / {@link AppointmentTimeConflictException} / {@link
   *     SlotUnavailableException} on the new slot.
   */
  @Transactional
  public BookingConfirmation reschedule(
      String callerCard, UUID authorAccountId, UUID appointmentId, LocalDateTime newSlot) {
    Appointment appointment = requireAccessibleAppointment(callerCard, appointmentId);
    Instant now = clock.instant();
    ZoneId zone = clock.getZone();
    if (!appointment.isChangeable(now)) {
      throw new AppointmentTooLateException();
    }

    ScheduleSlot target =
        resolveBookableSlot(
            appointment.getUnitId(),
            appointment.getType(),
            appointment.scopeCode(),
            newSlot,
            now,
            zone);
    Instant newScheduledAt =
        BookingHorizon.instantOf(newSlot.toLocalDate(), newSlot.toLocalTime(), zone);
    requireNoConflict(appointment.getBeneficiaryId(), newScheduledAt, appointment.getId());

    UUID oldSlotId = appointment.getSlotId();
    occupySeat(target);
    releaseSeat(oldSlotId);
    appointment.rescheduleTo(target.getId(), newScheduledAt, now);
    appointments.save(appointment);

    metrics.counter("appointment.rescheduled").increment();
    log.info(
        "appointment {} rescheduled for beneficiary {}",
        appointment.getProtocol(),
        mask(appointment.getBeneficiaryId()));
    events.publishEvent(
        new AppointmentRescheduled(
            appointment.getId(),
            appointment.getBeneficiaryId(),
            appointment.getProtocol(),
            appointment.getType().name(),
            appointment.scopeCode(),
            appointment.getUnitId(),
            newScheduledAt,
            authorAccountId));
    return new BookingConfirmation(appointment.getProtocol(), AppointmentStatus.REAGENDADO);
  }

  /**
   * Meus Agendamentos across all beneficiaries the caller may act for (BR13), optionally narrowed
   * to one of them: {@code upcoming} soonest-first and {@code history} most-recent-first, with
   * {@code REALIZADO} derived for past active items (BR12). When {@code telemedicineOnly} is set,
   * only {@code TELEMEDICINA} commitments are returned — the SPEC-0010 BR1/BR14 "Meus Agendamentos
   * (filtered by Telemedicina)" scope the tele feature reads with {@code telemedicine=true}.
   */
  @Transactional(readOnly = true)
  public AppointmentListResponse list(
      String callerCard, UUID beneficiaryFilter, boolean telemedicineOnly) {
    Map<UUID, String> namesById =
        beneficiaryAccess.accessibleFor(callerCard).stream()
            .collect(
                Collectors.toMap(
                    AccessibleBeneficiary::beneficiaryId, AccessibleBeneficiary::firstName));
    if (namesById.isEmpty()
        || (beneficiaryFilter != null && !namesById.containsKey(beneficiaryFilter))) {
      return new AppointmentListResponse(List.of(), List.of());
    }

    List<Appointment> found =
        (beneficiaryFilter != null
                ? appointments.findByBeneficiaryIdOrderByScheduledAtDesc(beneficiaryFilter)
                : appointments.findByBeneficiaryIdInOrderByScheduledAtDesc(namesById.keySet()))
            .stream()
                .filter(
                    a -> !telemedicineOnly || a.getModality() == AppointmentModality.TELEMEDICINA)
                .toList();

    Instant now = clock.instant();
    Map<UUID, String> unitNames = unitNamesFor(found);
    Map<String, String> examNames = examNamesFor(found);

    List<AppointmentView> upcoming =
        found.stream()
            .filter(a -> a.effectiveStatus(now).isActive())
            .sorted(Comparator.comparing(Appointment::getScheduledAt))
            .map(a -> toView(a, now, namesById, unitNames, examNames))
            .toList();
    List<AppointmentView> history =
        found.stream()
            .filter(a -> !a.effectiveStatus(now).isActive())
            .sorted(Comparator.comparing(Appointment::getScheduledAt).reversed())
            .map(a -> toView(a, now, namesById, unitNames, examNames))
            .toList();
    return new AppointmentListResponse(upcoming, history);
  }

  /**
   * The join-relevant projection of a scheduled appointment for the telemedicine module's room
   * (SPEC-0010 BR14, DL-0018): scope-checked so it never reveals an appointment outside the
   * caller's family, and reporting whether it is a Telemedicina booking still open. The entity
   * never leaves the module.
   *
   * @throws AppointmentNotFoundException when unknown or out of the caller's scope.
   */
  @Transactional(readOnly = true)
  public TeleJoinTarget teleJoinTarget(String callerCard, UUID appointmentId) {
    Appointment appointment = requireAccessibleAppointment(callerCard, appointmentId);
    return new TeleJoinTarget(
        appointment.getId(),
        appointment.getBeneficiaryId(),
        appointment.getScheduledAt(),
        appointment.getModality() == AppointmentModality.TELEMEDICINA,
        appointment.getStatus().isActive(),
        appointment.getCreatedBy());
  }

  /**
   * The modality a booking against {@code unitId} takes: TELEMEDICINA for the virtual tele unit.
   */
  private AppointmentModality modalityOf(UUID unitId) {
    return careUnits
        .findById(unitId)
        .filter(CareUnit::isVirtual)
        .map(unit -> AppointmentModality.TELEMEDICINA)
        .orElse(AppointmentModality.PRESENCIAL);
  }

  private String validatedScopeCode(BookAppointmentCommand command) {
    if (command.type() == AppointmentType.CONSULTATION) {
      String code = command.specialtyCode();
      if (code == null || !specialties.isValid(code)) {
        throw new AppointmentOutsideHorizonException();
      }
      return code;
    }
    String code = command.examCode();
    if (code == null || !examTypes.existsById(code)) {
      throw new AppointmentOutsideHorizonException();
    }
    return code;
  }

  private ScheduleSlot resolveBookableSlot(
      UUID unitId,
      AppointmentType type,
      String code,
      LocalDateTime slot,
      Instant now,
      ZoneId zone) {
    LocalDate date = slot.toLocalDate();
    LocalTime time = slot.toLocalTime();
    ScheduleSlot resolved =
        agendas
            .findByUnitIdAndScopeTypeAndScopeCode(unitId, type, code)
            .flatMap(
                agenda -> slots.findByAgendaIdAndSlotDateAndSlotTime(agenda.getId(), date, time))
            .orElseThrow(AppointmentOutsideHorizonException::new);
    if (!BookingHorizon.isBookable(date, time, now, zone)) {
      throw new AppointmentOutsideHorizonException();
    }
    return resolved;
  }

  private void requireNoConflict(
      UUID beneficiaryId, Instant scheduledAt, UUID excludingAppointmentId) {
    appointments
        .findFirstByBeneficiaryIdAndScheduledAtAndStatusIn(
            beneficiaryId, scheduledAt, ACTIVE_STATUSES)
        .filter(existing -> !existing.getId().equals(excludingAppointmentId))
        .ifPresent(
            existing -> {
              throw new AppointmentTimeConflictException();
            });
  }

  /**
   * Takes one seat, translating a lost last-seat race to {@link SlotUnavailableException}
   * fail-fast: the in-memory guard rejects an already-full slot, and the optimistic-lock conflict
   * at flush rejects the loser of two concurrent confirmations — neither is retried (BR6).
   */
  private void occupySeat(ScheduleSlot slot) {
    try {
      slot.occupy();
      slots.saveAndFlush(slot);
    } catch (OptimisticLockingFailureException race) {
      metrics.counter("appointment.slot_conflicts").increment();
      throw new SlotUnavailableException();
    } catch (SlotUnavailableException full) {
      metrics.counter("appointment.slot_conflicts").increment();
      throw full;
    }
  }

  private void releaseSeat(UUID slotId) {
    slots
        .findById(slotId)
        .ifPresent(
            slot -> {
              slot.release();
              slots.save(slot);
            });
  }

  private Appointment requireAccessibleAppointment(String callerCard, UUID appointmentId) {
    Appointment appointment =
        appointments.findById(appointmentId).orElseThrow(AppointmentNotFoundException::new);
    Set<UUID> accessible =
        beneficiaryAccess.accessibleFor(callerCard).stream()
            .map(AccessibleBeneficiary::beneficiaryId)
            .collect(Collectors.toSet());
    if (!accessible.contains(appointment.getBeneficiaryId())) {
      throw new AppointmentNotFoundException();
    }
    return appointment;
  }

  private AppointmentCancelled cancelledEvent(Appointment appointment, UUID authorAccountId) {
    return new AppointmentCancelled(
        appointment.getId(),
        appointment.getBeneficiaryId(),
        appointment.getProtocol(),
        appointment.getType().name(),
        appointment.scopeCode(),
        appointment.getUnitId(),
        appointment.getScheduledAt(),
        authorAccountId);
  }

  private Map<UUID, String> unitNamesFor(List<Appointment> found) {
    Set<UUID> unitIds = found.stream().map(Appointment::getUnitId).collect(Collectors.toSet());
    return careUnits.findAllById(unitIds).stream()
        .collect(Collectors.toMap(CareUnit::getId, CareUnit::getName));
  }

  private Map<String, String> examNamesFor(List<Appointment> found) {
    Set<String> examCodes =
        found.stream()
            .map(Appointment::getExamCode)
            .filter(code -> code != null)
            .collect(Collectors.toSet());
    return examTypes.findAllById(examCodes).stream()
        .collect(
            Collectors.toMap(
                ExamType::getCode, ExamType::getName, (a, b) -> a, java.util.HashMap::new));
  }

  private AppointmentView toView(
      Appointment appointment,
      Instant now,
      Map<UUID, String> names,
      Map<UUID, String> unitNames,
      Map<String, String> examNames) {
    return new AppointmentView(
        appointment.getId(),
        appointment.getProtocol(),
        appointment.getType(),
        appointment.getModality(),
        appointment.getSpecialtyCode(),
        appointment.getExamCode(),
        appointment.getExamCode() == null ? null : examNames.get(appointment.getExamCode()),
        appointment.getBeneficiaryId(),
        names.get(appointment.getBeneficiaryId()),
        appointment.getUnitId(),
        unitNames.get(appointment.getUnitId()),
        appointment.getScheduledAt(),
        appointment.effectiveStatus(now),
        appointment.getCancelReason());
  }

  /** Masks a beneficiary id in business logs (LGPD hygiene — security.md §Privacy). */
  private static String mask(UUID beneficiaryId) {
    String id = beneficiaryId.toString();
    return "***" + id.substring(id.length() - 4);
  }
}
