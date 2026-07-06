package com.fkmed.application.api;

import com.fkmed.application.api.dto.CancelAppointmentRequest;
import com.fkmed.application.api.dto.CreateAppointmentRequest;
import com.fkmed.application.api.dto.RescheduleAppointmentRequest;
import com.fkmed.domain.appointment.AppointmentListResponse;
import com.fkmed.domain.appointment.AppointmentService;
import com.fkmed.domain.appointment.AppointmentType;
import com.fkmed.domain.appointment.AvailabilityResponse;
import com.fkmed.domain.appointment.BookAppointmentCommand;
import com.fkmed.domain.appointment.BookingConfirmation;
import com.fkmed.domain.appointment.CareUnitView;
import com.fkmed.domain.appointment.ExamTypeView;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Appointment endpoints (SPEC-0009): unit and availability reads, booking (JSON for a consultation,
 * multipart when an exam attachment travels along), Meus Agendamentos, cancellation and
 * rescheduling. Family scope, slot capacity and the state machine are enforced server-side in
 * {@code domain.appointment.AppointmentService}; the caller's beneficiary card and the acting
 * account are resolved from the JWT, never client-supplied.
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

  private final AppointmentService appointments;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /** The exam catalog for the exam wizard (BR4), mirroring {@code GET /api/network/specialties}. */
  @GetMapping("/exams")
  List<ExamTypeView> exams() {
    return appointments.examCatalog();
  }

  /** Units serving a specialty or an exam (BR3/BR4). */
  @GetMapping("/units")
  List<CareUnitView> units(
      @RequestParam(required = false) String specialty,
      @RequestParam(required = false) String exam) {
    Scope scope = scope(specialty, exam);
    return appointments.unitsServing(scope.type(), scope.code());
  }

  /**
   * Days + time slots with remaining capacity for a (unit, scope), respecting horizon/antecedence
   * (BR5).
   */
  /** The bookable days for a (unit, scope) — a bare array the wizard's slot step renders (BR5). */
  @GetMapping("/availability")
  List<AvailabilityResponse.Day> availability(
      @RequestParam UUID unitId,
      @RequestParam(required = false) String specialty,
      @RequestParam(required = false) String exam) {
    Scope scope = scope(specialty, exam);
    return appointments.availability(unitId, scope.type(), scope.code()).days();
  }

  /** Confirms a consultation (JSON, no attachment) — BR7. */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  BookingConfirmation create(@Valid @RequestBody CreateAppointmentRequest request) {
    return appointments.book(request.toCommand(callerCard(), authorAccountId()), null, null);
  }

  /** Confirms a booking with a medical-order attachment (multipart) — the exam flow (BR4/BR7). */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  BookingConfirmation createWithAttachment(
      @RequestParam UUID beneficiaryId,
      @RequestParam AppointmentType type,
      @RequestParam(required = false) String specialty,
      @RequestParam(required = false) String exam,
      @RequestParam UUID unitId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot,
      @RequestPart(value = "medicalOrder", required = false) MultipartFile medicalOrder) {
    BookAppointmentCommand command =
        new BookAppointmentCommand(
            callerCard(), authorAccountId(), beneficiaryId, type, specialty, exam, unitId, slot);
    return appointments.book(command, bytesOf(medicalOrder), fileNameOf(medicalOrder));
  }

  /** Meus Agendamentos across all accessible beneficiaries, optionally filtered (BR13). */
  @GetMapping
  AppointmentListResponse list(@RequestParam(required = false) UUID beneficiaryId) {
    return appointments.list(callerCard(), beneficiaryId);
  }

  /** Cancels an upcoming appointment with an optional reason (BR9). */
  @PostMapping("/{id}/cancel")
  BookingConfirmation cancel(
      @PathVariable UUID id,
      @Valid @RequestBody(required = false) CancelAppointmentRequest request) {
    String reason = request == null ? null : request.reason();
    return appointments.cancel(callerCard(), authorAccountId(), id, reason);
  }

  /** Reschedules an upcoming appointment to a new slot, keeping the protocol (BR10). */
  @PostMapping("/{id}/reschedule")
  BookingConfirmation reschedule(
      @PathVariable UUID id, @Valid @RequestBody RescheduleAppointmentRequest request) {
    return appointments.reschedule(callerCard(), authorAccountId(), id, request.slot());
  }

  private static Scope scope(String specialty, String exam) {
    boolean hasSpecialty = specialty != null && !specialty.isBlank();
    boolean hasExam = exam != null && !exam.isBlank();
    if (hasSpecialty == hasExam) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "exactly one of 'specialty' or 'exam' is required");
    }
    return hasSpecialty
        ? new Scope(AppointmentType.CONSULTATION, specialty)
        : new Scope(AppointmentType.EXAM, exam);
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private UUID authorAccountId() {
    return accounts
        .findByEmail(userContext.current().username())
        .map(AccountCredentials::accountId)
        .orElse(null);
  }

  private static byte[] bytesOf(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("could not read the uploaded medical order", e);
    }
  }

  private static String fileNameOf(MultipartFile file) {
    return file == null ? null : file.getOriginalFilename();
  }

  private record Scope(AppointmentType type, String code) {}
}
