import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CreateSessionRequest, TeleApi, TeleCatalog, TeleSession } from './tele.api';

/**
 * SPEC-0010 §API Contracts — the FROZEN Phase-4 contract: verifies each endpoint's path, method,
 * query params and body/response shape exactly as the architect froze them (Phase-3 lesson: consume
 * the frozen shapes, do not reshape). Scheduled-teleconsultation calls reuse the SPEC-0009
 * appointment contract with the `telemedicine` modality scope (DL-0018).
 */
describe('TeleApi', () => {
  let api: TeleApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(TeleApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getCatalog() GETs /api/tele/catalog → {symptoms, term} (BR2/BR4)', () => {
    const catalog: TeleCatalog = {
      symptoms: [
        { code: 'CEFALEIA', name: 'Dor de cabeça' },
        { code: 'DOR_TORACICA', name: 'Dor no peito', emergency: true },
      ],
      term: { version: '1.0', body: 'Termo de teleatendimento.' },
    };
    let received: TeleCatalog | undefined;
    api.getCatalog().subscribe((r) => (received = r));
    http.expectOne('/api/tele/catalog').flush(catalog);
    expect(received).toEqual(catalog);
  });

  it('getActiveInstabilityNotice() returns the ALERT telemedicine notice from /api/content/home (BR1/AC7)', () => {
    let received: { title: string; body: string } | null | undefined;
    api.getActiveInstabilityNotice().subscribe((r) => (received = r));
    http.expectOne('/api/content/home').flush({
      banners: [],
      notices: [
        { title: 'Lei Geral de Proteção de Dados Pessoais', severity: 'INFORMATIVE', body: 'LGPD.' },
        { title: 'Instabilidade momentânea da Telemedicina', severity: 'ALERT', body: 'Estamos normalizando.' },
      ],
    });
    expect(received).toEqual({
      title: 'Instabilidade momentânea da Telemedicina',
      body: 'Estamos normalizando.',
    });
  });

  it('getActiveInstabilityNotice() returns null when no telemedicine ALERT is active', () => {
    let received: { title: string; body: string } | null | undefined = undefined;
    api.getActiveInstabilityNotice().subscribe((r) => (received = r));
    http.expectOne('/api/content/home').flush({
      notices: [{ title: 'Lei Geral de Proteção de Dados', severity: 'INFORMATIVE', body: 'LGPD.' }],
    });
    expect(received).toBeNull();
  });

  it('createSession() POSTs the frozen body and returns the session (BR5)', () => {
    const request: CreateSessionRequest = {
      beneficiaryId: 'b1',
      complaint: 'Dor de cabeça há 2 dias com febre baixa.',
      symptoms: ['CEFALEIA'],
      otherSymptom: 'tontura leve',
      duration: 'D1_3',
      termVersion: '1.0',
    };
    const session: TeleSession = { state: 'EM_FILA', position: 4, etaMinutes: 12 };
    let received: TeleSession | undefined;
    api.createSession(request).subscribe((r) => (received = r));
    const req = http.expectOne({ url: '/api/tele/sessions', method: 'POST' });
    expect(req.request.body).toEqual(request);
    req.flush(session);
    expect(received).toEqual(session);
  });

  it('getCurrentSession() GETs /api/tele/sessions/current (plain JSON fallback)', () => {
    const session: TeleSession = {
      state: 'EM_ATENDIMENTO',
      professional: { name: 'Dra. Ana', crm: 'CRM-RJ 123456' },
      room: { startedAt: '2026-07-06T15:00' },
    };
    let received: TeleSession | undefined;
    api.getCurrentSession().subscribe((r) => (received = r));
    http.expectOne({ url: '/api/tele/sessions/current', method: 'GET' }).flush(session);
    expect(received).toEqual(session);
  });

  it('leaveSession() POSTs /api/tele/sessions/current/leave (BR5/BR9)', () => {
    api.leaveSession().subscribe();
    const req = http.expectOne({ url: '/api/tele/sessions/current/leave', method: 'POST' });
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('joinScheduled() POSTs /api/appointments/{id}/join (BR14)', () => {
    api.joinScheduled('appt-1').subscribe();
    const req = http.expectOne({ url: '/api/appointments/appt-1/join', method: 'POST' });
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('getTeleAppointments() GETs /api/appointments with telemedicine=true (BR14)', () => {
    api.getTeleAppointments('b1').subscribe();
    const req = http.expectOne((r) => r.url === '/api/appointments');
    expect(req.request.params.get('telemedicine')).toBe('true');
    expect(req.request.params.get('beneficiaryId')).toBe('b1');
    req.flush({ upcoming: [], history: [] });
  });

  it('getTeleAvailability() scopes the SPEC-0009 availability with telemedicine=true, no unitId (BR14/DL-0018)', () => {
    api.getTeleAvailability('CARDIOLOGIA').subscribe();
    const req = http.expectOne((r) => r.url === '/api/appointments/availability');
    expect(req.request.params.get('specialty')).toBe('CARDIOLOGIA');
    expect(req.request.params.get('telemedicine')).toBe('true');
    expect(req.request.params.has('unitId')).toBe(false);
    req.flush([]);
  });

  it('bookTeleConsultation() POSTs a CONSULTATION with the telemedicine modality (BR14)', () => {
    api.bookTeleConsultation({ beneficiaryId: 'b1', specialty: 'CARDIOLOGIA', slot: '2026-07-10T09:00' }).subscribe();
    const req = http.expectOne({ url: '/api/appointments', method: 'POST' });
    expect(req.request.body).toEqual({
      beneficiaryId: 'b1',
      type: 'CONSULTATION',
      specialty: 'CARDIOLOGIA',
      slot: '2026-07-10T09:00',
      telemedicine: true,
    });
    req.flush({ protocol: 'AG-20260706-0001', status: 'AGENDADO' });
  });
});
