import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Domain-oriented API of the Appointments feature (SPEC-0009). No raw HttpClient in components
 * (frontend-angular.md §HTTP and errors). Built against the contract the architect froze in the
 * slice plan — see the header note on `getExams()` for the one endpoint not enumerated there.
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

/** BR13 card item — `beneficiary`/`unit`/`specialty`/`exam` are display names (`specialty`/`exam`
 * mutually exclusive by `type`); `telemedicine` drives the future SPEC-0010 badge (BR13),
 * absent/false for now.
 *
 * The frozen item list in the slice plan is terse; the reschedule action (BR10, "only date/time
 * reopens") needs the availability query params for the kept unit + scope, so the item is enriched
 * with the `unitId`/`specialtyCode`/`examCode` identifiers used only to reopen the calendar — this
 * reuses the frozen `/availability` endpoint (no new endpoint). Flagged to the architect to confirm
 * against the real OpenAPI snapshot at integration; kept optional so a divergent shape degrades
 * gracefully (reschedule simply cannot preselect without them). */
export interface Appointment {
  id: string;
  type: AppointmentType;
  specialty: string | null;
  exam: string | null;
  beneficiary: string;
  unit: string;
  scheduledAt: string;
  status: AppointmentStatus;
  protocol: string;
  telemedicine?: boolean;
  unitId?: string;
  specialtyCode?: string | null;
  examCode?: string | null;
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

  /**
   * BR4: the exam catalog (`exam_type` registry owned by `domain.appointment`, ADR-0012). The slice
   * plan's frozen contract enumerates `/units`, `/availability`, POST/GET `/appointments` and
   * cancel/reschedule but does NOT name the exam-catalog endpoint (it names `/api/network/specialties`
   * for the consultation side). Built here against the symmetric `/api/appointments/exams` returning
   * the same `{code,name}` registry shape — flagged to the architect to confirm or re-sync against
   * the real OpenAPI snapshot at integration. Isolated in this single method so re-pointing is trivial.
   */
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

  /** BR4/BR7: confirm an exam as multipart carrying the mandatory medical-order file (field `file`,
   * mirroring the profile-photo upload convention). */
  bookExam(booking: ExamBooking): Observable<BookingConfirmation> {
    const form = new FormData();
    form.append('beneficiaryId', booking.beneficiaryId);
    form.append('type', 'EXAM');
    form.append('exam', booking.exam);
    form.append('unitId', booking.unitId);
    form.append('slot', booking.slot);
    form.append('file', booking.file);
    return this.http.post<BookingConfirmation>('/api/appointments', form);
  }

  /** BR13: all accessible beneficiaries' commitments; `beneficiaryId` narrows to one. */
  getAppointments(beneficiaryId?: string): Observable<Appointment[]> {
    let params = new HttpParams();
    if (beneficiaryId) {
      params = params.set('beneficiaryId', beneficiaryId);
    }
    return this.http.get<Appointment[]>('/api/appointments', { params });
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
