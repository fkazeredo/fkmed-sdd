import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  AppointmentList,
  AppointmentsApi,
  AvailabilityDay,
  BookingConfirmation,
  CareUnit,
  RegistryOption,
} from './appointments.api';

/**
 * SPEC-0009 §API Contracts, reconciled to the REAL backend at integration: verifies each endpoint's
 * path, HTTP method, query params, JSON vs multipart body (part `medicalOrder`) and response shape
 * (`GET /api/appointments` → `{upcoming,history}`; raw arrays for the registry/unit/availability lists).
 */
describe('AppointmentsApi', () => {
  let api: AppointmentsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(AppointmentsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getSpecialties() calls GET /api/network/specialties (shared registry, BR3)', () => {
    const result: RegistryOption[] = [{ code: 'CARDIOLOGIA', name: 'Cardiologia' }];
    let received: RegistryOption[] | undefined;
    api.getSpecialties().subscribe((r) => (received = r));
    http.expectOne('/api/network/specialties').flush(result);
    expect(received).toEqual(result);
  });

  it('getExams() calls GET /api/appointments/exams (exam_type registry, BR4)', () => {
    const result: RegistryOption[] = [{ code: 'HEMOGRAMA', name: 'Hemograma' }];
    let received: RegistryOption[] | undefined;
    api.getExams().subscribe((r) => (received = r));
    http.expectOne('/api/appointments/exams').flush(result);
    expect(received).toEqual(result);
  });

  it('getUnits() sends specialty when given', () => {
    const result: CareUnit[] = [{ id: 'u1', name: 'Unidade Centro', address: 'Rua A, 10 — Centro' }];
    let received: CareUnit[] | undefined;
    api.getUnits({ specialty: 'CARDIOLOGIA' }).subscribe((r) => (received = r));
    const req = http.expectOne((request) => request.url === '/api/appointments/units');
    expect(req.request.params.get('specialty')).toBe('CARDIOLOGIA');
    expect(req.request.params.has('exam')).toBe(false);
    req.flush(result);
    expect(received).toEqual(result);
  });

  it('getUnits() sends exam when given', () => {
    api.getUnits({ exam: 'HEMOGRAMA' }).subscribe();
    const req = http.expectOne((request) => request.url === '/api/appointments/units');
    expect(req.request.params.get('exam')).toBe('HEMOGRAMA');
    expect(req.request.params.has('specialty')).toBe(false);
    req.flush([]);
  });

  it('getAvailability() sends unitId and the specialty/exam scope (BR5)', () => {
    const days: AvailabilityDay[] = [
      { date: '2026-07-10', slots: [{ slot: '2026-07-10T09:00', remaining: 2, available: true }] },
    ];
    let received: AvailabilityDay[] | undefined;
    api.getAvailability({ unitId: 'u1', specialty: 'CARDIOLOGIA' }).subscribe((r) => (received = r));
    const req = http.expectOne((request) => request.url === '/api/appointments/availability');
    expect(req.request.params.get('unitId')).toBe('u1');
    expect(req.request.params.get('specialty')).toBe('CARDIOLOGIA');
    req.flush(days);
    expect(received).toEqual(days);
  });

  it('bookConsultation() POSTs the JSON body with type CONSULTATION (BR7)', () => {
    const confirmation: BookingConfirmation = { protocol: 'AG-20260704-0001', status: 'AGENDADO' };
    let received: BookingConfirmation | undefined;
    api
      .bookConsultation({ beneficiaryId: 'b1', specialty: 'CARDIOLOGIA', unitId: 'u1', slot: '2026-07-10T09:00' })
      .subscribe((r) => (received = r));
    const req = http.expectOne({ url: '/api/appointments', method: 'POST' });
    expect(req.request.body).toEqual({
      beneficiaryId: 'b1',
      type: 'CONSULTATION',
      specialty: 'CARDIOLOGIA',
      unitId: 'u1',
      slot: '2026-07-10T09:00',
    });
    req.flush(confirmation);
    expect(received).toEqual(confirmation);
  });

  it('bookExam() POSTs multipart FormData with the file as part "medicalOrder" and type EXAM (BR4/BR7)', () => {
    const file = new Blob(['order'], { type: 'application/pdf' });
    api
      .bookExam({ beneficiaryId: 'b1', exam: 'HEMOGRAMA', unitId: 'u1', slot: '2026-07-10T09:00', file })
      .subscribe();
    const req = http.expectOne({ url: '/api/appointments', method: 'POST' });
    const body = req.request.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get('type')).toBe('EXAM');
    expect(body.get('exam')).toBe('HEMOGRAMA');
    expect(body.get('unitId')).toBe('u1');
    expect(body.get('slot')).toBe('2026-07-10T09:00');
    expect(body.get('medicalOrder')).toBeInstanceOf(Blob);
    expect(body.get('file')).toBeNull(); // reconciled: the backend part is `medicalOrder`
    req.flush({ protocol: 'AG-20260704-0002', status: 'AGENDADO' });
  });

  it('getAppointments() omits beneficiaryId when not given and returns {upcoming,history} (BR13)', () => {
    const list: AppointmentList = { upcoming: [], history: [] };
    let received: AppointmentList | undefined;
    api.getAppointments().subscribe((r) => (received = r));
    const req = http.expectOne((request) => request.url === '/api/appointments');
    expect(req.request.params.has('beneficiaryId')).toBe(false);
    req.flush(list);
    expect(received).toEqual(list);
  });

  it('getAppointments() sends beneficiaryId when filtering (BR13)', () => {
    api.getAppointments('b1').subscribe();
    const req = http.expectOne((request) => request.url === '/api/appointments');
    expect(req.request.params.get('beneficiaryId')).toBe('b1');
    req.flush({ upcoming: [], history: [] });
  });

  it('cancel() POSTs the reason when given (BR9)', () => {
    api.cancel('a1', 'Imprevisto').subscribe();
    const req = http.expectOne({ url: '/api/appointments/a1/cancel', method: 'POST' });
    expect(req.request.body).toEqual({ reason: 'Imprevisto' });
    req.flush(null);
  });

  it('cancel() POSTs an empty body when no reason (BR9)', () => {
    api.cancel('a1').subscribe();
    const req = http.expectOne({ url: '/api/appointments/a1/cancel', method: 'POST' });
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('reschedule() POSTs only the new slot (BR10, same protocol)', () => {
    const confirmation: BookingConfirmation = { protocol: 'AG-20260704-0001', status: 'REAGENDADO' };
    let received: BookingConfirmation | undefined;
    api.reschedule('a1', '2026-07-11T10:00').subscribe((r) => (received = r));
    const req = http.expectOne({ url: '/api/appointments/a1/reschedule', method: 'POST' });
    expect(req.request.body).toEqual({ slot: '2026-07-11T10:00' });
    req.flush(confirmation);
    expect(received).toEqual(confirmation);
  });
});
