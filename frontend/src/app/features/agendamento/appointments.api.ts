import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Domain-oriented API of the Appointments feature (SPEC-0009). No raw HttpClient in components
 * (frontend-angular.md §HTTP and errors). Reconciled to the REAL backend contract at integration:
 * JSON `POST /api/appointments` for consultations, multipart (part `medicalOrder`) for exams,
 * `GET /api/appointments` returning `{upcoming,history}`, and `GET /api/appointments/exams`.
 */

export type AppointmentType = 'CONSULTATION' | 'EXAM';

/** BR11 lifecycle. */
export type AppointmentStatus = 'AGENDADO' | 'REAGENDADO' | 'CANCELADO' | 'REALIZADO';

/** Registry data (specialty/exam): the string code is the value; `name` is the label (BR3/BR4). */
export interface RegistryOption {
  code: string;
  name: string;
}

/** BR3/BR4 unit list — `address` arrives as a single pre-formatted string (same convention the
 * network backend adopted for `locality`, SPEC-0008 integration). */
export interface CareUnit {
  id: string;
  name: string;
  address: string;
}

/** One bookable time within a day (BR5/BR6): the ISO local datetime, the remaining capacity, and
 * an `available` flag the backend already computes (full → `false`, and only slots ≥ 2 h ahead
 * within today→+30 d are returned — DL-0013). Full slots MUST render unselectable (BR5). */
export interface AvailabilitySlot {
  slot: string;
  remaining: number;
  available: boolean;
}

/** A day of the unit's agenda with its time slots (BR5). Only today→+30 d days come back. */
export interface AvailabilityDay {
  date: string;
  slots: AvailabilitySlot[];
}

/** BR13 card item — the REAL backend `AppointmentView` (integration reconcile): identifiers plus
 * display names, `specialtyCode`/`examCode`+`examName` mutually exclusive by `type`, and the
 * `unitId`/`specialtyCode`/`examCode` the reschedule action reuses to reopen `/availability` (BR10).
 * `telemedicine` drives the future SPEC-0010 badge (BR13), absent/false for now.
 *
 * `specialtyName` is kept optional and the card falls back to `specialtyCode` when the view omits it
 * — the backend snapshot was not yet on this base to confirm the field, so display degrades safely
 * either way (flagged to the architect). */
export interface AppointmentView {
  id: string;
  protocol: string;
  type: AppointmentType;
  specialtyCode: string | null;
  specialtyName?: string | null;
  examCode: string | null;
  examName: string | null;
  beneficiaryId: string;
  beneficiaryName: string;
  unitId: string;
  unitName: string;
  scheduledAt: string;
  status: AppointmentStatus;
  telemedicine?: boolean;
}

/** BR13: the backend already splits and orders — Próximos (`upcoming`) soonest-first, Histórico
 * (`history`) most-recent-first — so the client consumes both lists as-is (no client-side split/sort). */
export interface AppointmentList {
  upcoming: AppointmentView[];
  history: AppointmentView[];
}

/** BR7 confirmation payload (`201 {protocol, status:"AGENDADO"}`); reschedule keeps the protocol
 * and returns `status:"REAGENDADO"` (BR10). */
export interface BookingConfirmation {
  protocol: string;
  status: AppointmentStatus;
}

export interface ConsultationBooking {
  beneficiaryId: string;
  specialty: string;
  unitId: string;
  slot: string;
}

export interface ExamBooking {
  beneficiaryId: string;
  exam: string;
  unitId: string;
  slot: string;
  file: Blob;
}

@Injectable({ providedIn: 'root' })
export class AppointmentsApi {
  private readonly http = inject(HttpClient);

  /** BR3: specialties are the registry shared with SPEC-0008 (`domain.network`, ADR-0012). */
  getSpecialties(): Observable<RegistryOption[]> {
    return this.http.get<RegistryOption[]>('/api/network/specialties');
  }

  /** BR4: the exam catalog (`exam_type` registry owned by `domain.appointment`, ADR-0012) —
   * `GET /api/appointments/exams` → raw `{code,name}[]`, confirmed against the real backend. */
  getExams(): Observable<RegistryOption[]> {
    return this.http.get<RegistryOption[]>('/api/appointments/exams');
  }

  /** BR3/BR4: own units serving the chosen specialty OR exam. */
  getUnits(scope: { specialty?: string; exam?: string }): Observable<CareUnit[]> {
    return this.http.get<CareUnit[]>('/api/appointments/units', { params: this.scopeParams(scope) });
  }

  /** BR5: days + time slots with remaining capacity for a unit within the specialty/exam scope. */
  getAvailability(query: {
    unitId: string;
    specialty?: string;
    exam?: string;
  }): Observable<AvailabilityDay[]> {
    const params = this.scopeParams(query).set('unitId', query.unitId);
    return this.http.get<AvailabilityDay[]>('/api/appointments/availability', { params });
  }

  /** BR7: confirm a consultation as JSON. */
  bookConsultation(booking: ConsultationBooking): Observable<BookingConfirmation> {
    return this.http.post<BookingConfirmation>('/api/appointments', {
      beneficiaryId: booking.beneficiaryId,
      type: 'CONSULTATION',
      specialty: booking.specialty,
      unitId: booking.unitId,
      slot: booking.slot,
    });
  }

  /** BR4/BR7: confirm an exam as multipart carrying the mandatory medical-order file. The backend
   * reads it as `@RequestPart("medicalOrder")`; the other parts are flat form fields. */
  bookExam(booking: ExamBooking): Observable<BookingConfirmation> {
    const form = new FormData();
    form.append('beneficiaryId', booking.beneficiaryId);
    form.append('type', 'EXAM');
    form.append('exam', booking.exam);
    form.append('unitId', booking.unitId);
    form.append('slot', booking.slot);
    form.append('medicalOrder', booking.file);
    return this.http.post<BookingConfirmation>('/api/appointments', form);
  }

  /** BR13: all accessible beneficiaries' commitments, already split (`upcoming`/`history`) and
   * ordered by the backend; `beneficiaryId` narrows to one. */
  getAppointments(beneficiaryId?: string): Observable<AppointmentList> {
    let params = new HttpParams();
    if (beneficiaryId) {
      params = params.set('beneficiaryId', beneficiaryId);
    }
    return this.http.get<AppointmentList>('/api/appointments', { params });
  }

  /** BR9: cancel until the start time; optional reason ≤ 200 chars. */
  cancel(id: string, reason?: string): Observable<void> {
    return this.http.post<void>(`/api/appointments/${id}/cancel`, reason ? { reason } : {});
  }

  /** BR10: reschedule keeps the protocol; only a new slot is sent. */
  reschedule(id: string, slot: string): Observable<BookingConfirmation> {
    return this.http.post<BookingConfirmation>(`/api/appointments/${id}/reschedule`, { slot });
  }

  private scopeParams(scope: { specialty?: string; exam?: string }): HttpParams {
    let params = new HttpParams();
    if (scope.specialty) {
      params = params.set('specialty', scope.specialty);
    }
    if (scope.exam) {
      params = params.set('exam', scope.exam);
    }
    return params;
  }
}
