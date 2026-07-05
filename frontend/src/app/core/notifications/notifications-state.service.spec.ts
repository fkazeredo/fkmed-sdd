import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificationsStateService } from './notifications-state.service';

/** SPEC-0004 BR2: the bell counter reflects unread notifications and updates immediately when
 * items are marked read — this service is the single source of truth both the bell and the
 * notification center rely on. */
describe('NotificationsStateService', () => {
  let service: NotificationsStateService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NotificationsStateService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts at 0 before anything loads', () => {
    expect(service.unread()).toBe(0);
  });

  it('refreshUnread loads the unread count from a minimal (size=1) page fetch', () => {
    service.refreshUnread();
    const req = http.expectOne(
      (request) => request.url === '/api/notifications' && request.method === 'GET',
    );
    expect(req.request.params.get('size')).toBe('1');
    req.flush({ unread: 3, items: [] });

    expect(service.unread()).toBe(3);
  });

  it('markRead decrements the counter immediately on success (BR2)', () => {
    service.syncUnread(2);
    service.markRead('notif-1').subscribe();
    http.expectOne({ url: '/api/notifications/notif-1/read', method: 'POST' }).flush(null);

    expect(service.unread()).toBe(1);
  });

  it('markRead never drops the counter below 0', () => {
    service.syncUnread(0);
    service.markRead('notif-1').subscribe();
    http.expectOne({ url: '/api/notifications/notif-1/read', method: 'POST' }).flush(null);

    expect(service.unread()).toBe(0);
  });

  it('markAllRead zeroes the counter on success (AC2)', () => {
    service.syncUnread(5);
    service.markAllRead().subscribe();
    http.expectOne({ url: '/api/notifications/read-all', method: 'POST' }).flush(null);

    expect(service.unread()).toBe(0);
  });

  it('does not decrement when the read call fails', () => {
    service.syncUnread(2);
    service.markRead('notif-1').subscribe({ error: () => undefined });
    http
      .expectOne({ url: '/api/notifications/notif-1/read', method: 'POST' })
      .flush({ code: 'notification.not-found' }, { status: 404, statusText: 'Not Found' });

    expect(service.unread()).toBe(2);
  });
});
