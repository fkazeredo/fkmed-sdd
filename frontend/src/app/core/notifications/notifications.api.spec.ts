import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  NotificationItem,
  NotificationListResponse,
  NotificationPreference,
  NotificationPreferencesResponse,
  NotificationsApi,
} from './notifications.api';

const REIMBURSEMENT_PAID: NotificationItem = {
  id: 'notif-1',
  type: 'reimbursement.paid',
  title: 'Reembolso pago',
  body: 'Seu reembolso RE-20260601-0001 foi pago: R$ 120,00.',
  link: '/reembolso/RE-20260601-0001',
  createdAt: '2026-07-01T10:00:00Z',
  read: false,
};

/** SPEC-0004: notification endpoints. Frozen contract (architect's slice plan) — no committed
 * OpenAPI snapshot yet (backend module not built on this branch). */
describe('NotificationsApi', () => {
  let api: NotificationsApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(NotificationsApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/notifications?page=&size= and returns unread + items', () => {
    const payload: NotificationListResponse = { unread: 2, items: [REIMBURSEMENT_PAID] };
    let result: NotificationListResponse | undefined;
    api.list(0, 20).subscribe((response) => (result = response));

    const req = http.expectOne(
      (request) => request.url === '/api/notifications' && request.method === 'GET',
    );
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(payload);

    expect(result).toEqual(payload);
  });

  it('defaults size to 20 when not given', () => {
    api.list(0).subscribe();
    const req = http.expectOne(
      (request) => request.url === '/api/notifications' && request.method === 'GET',
    );
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ unread: 0, items: [] });
  });

  it('calls POST /api/notifications/{id}/read', () => {
    let done = false;
    api.markRead('notif-1').subscribe(() => (done = true));
    http.expectOne({ url: '/api/notifications/notif-1/read', method: 'POST' }).flush(null);
    expect(done).toBe(true);
  });

  it('calls POST /api/notifications/read-all', () => {
    let done = false;
    api.markAllRead().subscribe(() => (done = true));
    http.expectOne({ url: '/api/notifications/read-all', method: 'POST' }).flush(null);
    expect(done).toBe(true);
  });

  it('calls GET /api/notifications/preferences and unwraps the {preferences:[...]} envelope', () => {
    const catalog: NotificationPreference[] = [
      { type: 'reimbursement.paid', description: 'Reembolso pago', emailOptOut: false, mandatory: false },
      { type: 'auth.password-changed', description: 'Senha alterada', emailOptOut: false, mandatory: true },
    ];
    const payload: NotificationPreferencesResponse = { preferences: catalog };
    let result: NotificationPreference[] | undefined;
    api.getPreferences().subscribe((response) => (result = response));

    http.expectOne({ url: '/api/notifications/preferences', method: 'GET' }).flush(payload);

    expect(result).toEqual(catalog);
  });

  it('calls PUT /api/notifications/preferences with a {preferences:[{type,emailOptOut}]} batch and returns the updated catalog', () => {
    const updated: NotificationPreference[] = [
      { type: 'reimbursement.paid', description: 'Reembolso pago', emailOptOut: true, mandatory: false },
    ];
    let result: NotificationPreference[] | undefined;
    api.updatePreference('reimbursement.paid', true).subscribe((response) => (result = response));

    const req = http.expectOne({ url: '/api/notifications/preferences', method: 'PUT' });
    expect(req.request.body).toEqual({ preferences: [{ type: 'reimbursement.paid', emailOptOut: true }] });
    req.flush({ preferences: updated });

    expect(result).toEqual(updated);
  });
});
