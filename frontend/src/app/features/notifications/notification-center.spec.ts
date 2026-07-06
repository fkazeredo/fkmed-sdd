import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { NotificationItem, NotificationListResponse } from '../../core/notifications/notifications.api';
import { NotificationsStateService } from '../../core/notifications/notifications-state.service';
import { NotificationCenter } from './notification-center';

function item(overrides: Partial<NotificationItem> = {}): NotificationItem {
  return {
    id: 'notif-1',
    type: 'reimbursement.paid',
    title: 'Reembolso pago',
    body: 'Seu reembolso RE-20260601-0001 foi pago: R$ 120,00.',
    link: '/reembolso/RE-20260601-0001',
    createdAt: '2026-07-01T10:00:00Z',
    read: false,
    ...overrides,
  };
}

/** SPEC-0004: notification center — BR1 (fields), BR2 (bell sync), BR3 (newest-first,
 * pagination), loading/empty/error states, mark-one-read and mark-all-read. */
describe('NotificationCenter', () => {
  let http: HttpTestingController;
  let state: NotificationsStateService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationCenter],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideI18n()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    state = TestBed.inject(NotificationsStateService);
  });

  afterEach(() => http.verify());

  async function setup(): Promise<ComponentFixture<NotificationCenter>> {
    const fixture = TestBed.createComponent(NotificationCenter);
    await fixture.whenStable();
    return fixture;
  }

  function flushFirstPage(response: NotificationListResponse): void {
    http
      .expectOne((request) => request.url === '/api/notifications' && request.params.get('page') === '0')
      .flush(response);
  }

  it('shows the loading state before the list resolves', async () => {
    const fixture = await setup();
    expect(fixture.nativeElement.textContent).toContain('Carregando…');
    http.expectOne((request) => request.url === '/api/notifications').flush({ unread: 0, items: [] });
  });

  it('shows the empty state when there are no notifications', async () => {
    const fixture = await setup();
    flushFirstPage({ unread: 0, items: [] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="notifications-empty"]')).not.toBeNull();
  });

  it('shows an error state with retry on failure', async () => {
    const fixture = await setup();
    http
      .expectOne((request) => request.url === '/api/notifications')
      .flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os dados. Tente novamente.');

    fixture.nativeElement.querySelector('button')?.click();
    flushFirstPage({ unread: 1, items: [item()] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-testid="notification-item"]')).toHaveLength(1);
  });

  it('renders items newest-first as returned by the API, with title/body/date and a deep link (BR1/BR3)', async () => {
    const fixture = await setup();
    const newer = item({ id: 'notif-2', title: 'Guia autorizada', createdAt: '2026-07-02T09:00:00Z' });
    const older = item({ id: 'notif-1', createdAt: '2026-07-01T10:00:00Z' });
    flushFirstPage({ unread: 2, items: [newer, older] });
    await fixture.whenStable();
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('[data-testid="notification-item"]');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Guia autorizada');
    expect(rows[1].textContent).toContain('Reembolso pago');
    const link = rows[1].querySelector('[data-testid="notification-link"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/reembolso/RE-20260601-0001');
  });

  it('syncs the shared bell counter from the page response (BR2)', async () => {
    const fixture = await setup();
    flushFirstPage({ unread: 4, items: [item()] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(state.unread()).toBe(4);
  });

  it('marking one item as read updates its style, hides the button and decrements the bell (BR2)', async () => {
    const fixture = await setup();
    flushFirstPage({ unread: 1, items: [item()] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(state.unread()).toBe(1);
    (fixture.nativeElement.querySelector('[data-testid="notification-mark-read"]') as HTMLElement).click();
    http.expectOne({ url: '/api/notifications/notif-1/read', method: 'POST' }).flush(null);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(state.unread()).toBe(0);
    expect(fixture.nativeElement.querySelector('[data-testid="notification-mark-read"]')).toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="notification-item"]')?.getAttribute('data-read'),
    ).toBe('true');
  });

  it('mark-all-read clears every unread item and zeroes the bell (AC2)', async () => {
    const fixture = await setup();
    flushFirstPage({ unread: 2, items: [item({ id: 'a' }), item({ id: 'b' })] });
    await fixture.whenStable();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('[data-testid="notifications-mark-all"]') as HTMLElement).click();
    http.expectOne({ url: '/api/notifications/read-all', method: 'POST' }).flush(null);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(state.unread()).toBe(0);
    expect(fixture.nativeElement.querySelectorAll('[data-testid="notification-mark-read"]')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('[data-testid="notifications-mark-all"]')).toBeNull();
  });

  it('shows "carregar mais" only when the page came back full, and appends the next page', async () => {
    const fixture = await setup();
    const fullPage = Array.from({ length: 20 }, (_, i) => item({ id: `n${i}`, title: `Item ${i}` }));
    flushFirstPage({ unread: 20, items: fullPage });
    await fixture.whenStable();
    fixture.detectChanges();

    const loadMore = fixture.nativeElement.querySelector(
      '[data-testid="notifications-load-more"]',
    ) as HTMLElement;
    expect(loadMore).not.toBeNull();

    loadMore.click();
    http
      .expectOne((request) => request.url === '/api/notifications' && request.params.get('page') === '1')
      .flush({ unread: 20, items: [item({ id: 'extra', title: 'Extra' })] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('[data-testid="notification-item"]')).toHaveLength(21);
    // Last page had fewer than the page size — no further "carregar mais".
    expect(fixture.nativeElement.querySelector('[data-testid="notifications-load-more"]')).toBeNull();
  });

  it('does not show "carregar mais" when the first page already has fewer items than the page size', async () => {
    const fixture = await setup();
    flushFirstPage({ unread: 1, items: [item()] });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="notifications-load-more"]')).toBeNull();
  });
});
