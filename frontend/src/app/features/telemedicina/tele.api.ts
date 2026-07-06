import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import {
  AppointmentList,
  AvailabilityDay,
  BookingConfirmation,
  RegistryOption,
} from '../agendamento/appointments.api';

/**
 * Domain-oriented API of the Telemedicine feature (SPEC-0010). No raw HttpClient in components
 * (frontend-angular.md §HTTP and errors). Consumes the contract the architect FROZE in the Phase-4
 * plan; the live SSE stream is handled by `TeleSessionStreamService`, this service exposes the same
 * `GET /api/tele/sessions/current` as a plain JSON read (fallback / tests).
 *
 * Contract notes flagged for backend confirmation at integration (Phase-3 lesson — consume the frozen
 * shapes, note the assumptions, do not silently reshape):
 *  - `TeleSymptom.emergency` is an OPTIONAL additive flag on the frozen `{code,name}` symptom shape —
 *    BR3 needs to know which symptoms are emergency signals and the catalog is the only place that
 *    data can live. Consumed defensively (absent → no alert). Backend must expose it.
 *  - `TeleSession.room` (start time + closure summary: duration, guidance, issued documents) is the
 *    room/summary container behind the frozen top-level `room?`; its exact composition is assumed
 *    from BR9 and must be confirmed.
 *  - Scheduled teleconsultation reuses the SPEC-0009 appointment contract (DL-0018): a `telemedicine`
 *    modality scope on units/availability/booking. The scoping mechanism (flag vs virtual unit id)
 *    is the one genuinely-new detail to confirm.
 */

/** BR11 state machine (enum on the backend). Finals: ENCERRADA, ABANDONADA. */
export type SessionState = 'EM_FILA' | 'EM_ATENDIMENTO' | 'ENCERRADA' | 'ABANDONADA';

/** BR2 symptom duration — fixed list; the code is the value, the label is i18n. */
export type SymptomDuration = 'HORAS' | 'D1_3' | 'D3_MAIS' | 'SEMANA_MAIS';

/** BR2 registry symptom. `emergency` is the optional additive flag (see class note) driving BR3. */
export interface TeleSymptom {
  code: string;
  name: string;
  emergency?: boolean;
}

/** BR4 versioned teleattendance term. */
export interface TeleTerm {
  version: string;
  body: string;
}

/** `GET /api/tele/catalog` → symptom registry + current term (frozen). */
export interface TeleCatalog {
  symptoms: TeleSymptom[];
  term: TeleTerm;
}

/** BR9 professional identity shown in the room. */
export interface TeleProfessional {
  name: string;
  crm: string;
}

/** A document issued at closure (BR9/BR10) — bound to the session, opened in Minha Saúde (SPEC-0011). */
export interface IssuedDocument {
  id: string;
  type: string;
  description?: string;
}

/** Room + closure-summary container (assumed shape behind the frozen top-level `room?` — see note). */
export interface TeleRoom {
  startedAt?: string;
  durationMinutes?: number;
  guidance?: string;
  documents?: IssuedDocument[];
}

/** `GET /api/tele/sessions/current` payload — the SAME shape whether read as JSON or streamed via SSE. */
export interface TeleSession {
  state: SessionState;
  position?: number;
  etaMinutes?: number;
  professional?: TeleProfessional;
  room?: TeleRoom;
}

/** BR1/AC7: the operator-authored instability notice shown as the hub/queue banner. Sourced from the
 * SPEC-0005 content notices (`/api/content/home`) — the only place this signal lives. */
export interface InstabilityNotice {
  title: string;
  body: string;
}

/** `POST /api/tele/sessions` body (frozen). */
export interface CreateSessionRequest {
  beneficiaryId: string;
  complaint: string;
  symptoms: string[];
  otherSymptom?: string;
  duration: SymptomDuration;
  termVersion: string;
}

@Injectable({ providedIn: 'root' })
export class TeleApi {
  private readonly http = inject(HttpClient);

  /** BR2/BR4: symptom registry + current term text/version. */
  getCatalog(): Observable<TeleCatalog> {
    return this.http.get<TeleCatalog>('/api/tele/catalog');
  }

  /** BR1/AC7: the active "Instabilidade momentânea da Telemedicina" notice (SPEC-0005 content), or
   * `null` when none is active. Matched by an ALERT-severity notice mentioning Telemedicina, so an
   * operator edit to the wording still resolves; the returned title/body ARE the operator content,
   * rendered verbatim in the informative (never blocking) banner. */
  getActiveInstabilityNotice(): Observable<InstabilityNotice | null> {
    return this.http
      .get<{ notices?: { title: string; severity: string; body: string }[] }>('/api/content/home')
      .pipe(
        map((content) => {
          const notice = (content.notices ?? []).find(
            (candidate) => candidate.severity === 'ALERT' && /telemedicina/i.test(candidate.title),
          );
          return notice ? { title: notice.title, body: notice.body } : null;
        }),
      );
  }

  /** BR5/BR7: enter the queue (or resume the existing session — the backend answers 200 with the
   * existing session, 201 with a new one; both deserialize to a `TeleSession`). */
  createSession(request: CreateSessionRequest): Observable<TeleSession> {
    return this.http.post<TeleSession>('/api/tele/sessions', request);
  }

  /** Plain JSON read of the current session (SSE-less fallback / tests). Streaming is the default
   * live path — `TeleSessionStreamService`. `404 tele.session-not-found` when there is none. */
  getCurrentSession(): Observable<TeleSession> {
    return this.http.get<TeleSession>('/api/tele/sessions/current');
  }

  /** BR5/BR9: leave the current session → ABANDONADA, position released. Backs both "Sair da fila"
   * (from the queue) and "Encerrar minha participação" (from the room) — the frozen contract exposes
   * a single leave endpoint for the current session, regardless of prior state. */
  leaveSession(): Observable<void> {
    return this.http.post<void>('/api/tele/sessions/current/leave', {});
  }

  /** BR14: open the scheduled teleconsultation room; `409 tele.join-window-closed` outside the
   * 10-min-before → end window. */
  joinScheduled(appointmentId: string): Observable<void> {
    return this.http.post<void>(`/api/appointments/${appointmentId}/join`, {});
  }

  // --- Scheduled teleconsultation on the SPEC-0009 appointment contract (DL-0018) ---

  /** BR14: telemedicine appointments (both beneficiaries) — reuses the SPEC-0009 list, split
   * `upcoming`/`history` server-side; the caller keeps only the telemedicine-modality items. */
  getTeleAppointments(beneficiaryId?: string): Observable<AppointmentList> {
    let params = new HttpParams().set('telemedicine', 'true');
    if (beneficiaryId) {
      params = params.set('beneficiaryId', beneficiaryId);
    }
    return this.http.get<AppointmentList>('/api/appointments', { params });
  }

  /** BR14: specialties offered in telemedicine (shared registry — same `/api/network/specialties`
   * as SPEC-0009; a tele-availability subset is a backend concern). */
  getTeleSpecialties(): Observable<RegistryOption[]> {
    return this.http.get<RegistryOption[]>('/api/network/specialties');
  }

  /** BR14: the tele agenda (30-day horizon) for a specialty. Reuses the SPEC-0009 availability
   * endpoint scoped with `telemedicine=true`; the backend resolves the virtual Telemedicina unit
   * (DL-0018), so no `unitId` is sent. */
  getTeleAvailability(specialty: string): Observable<AvailabilityDay[]> {
    const params = new HttpParams().set('specialty', specialty).set('telemedicine', 'true');
    return this.http.get<AvailabilityDay[]>('/api/appointments/availability', { params });
  }

  /** BR14: confirm a scheduled teleconsultation — a SPEC-0009 CONSULTATION with the telemedicine
   * modality; the backend binds it to the virtual Telemedicina unit (DL-0018) and applies the badge. */
  bookTeleConsultation(booking: {
    beneficiaryId: string;
    specialty: string;
    slot: string;
  }): Observable<BookingConfirmation> {
    return this.http.post<BookingConfirmation>('/api/appointments', {
      beneficiaryId: booking.beneficiaryId,
      type: 'CONSULTATION',
      specialty: booking.specialty,
      slot: booking.slot,
      telemedicine: true,
    });
  }
}
