import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NotificationsStateService } from '../notifications/notifications-state.service';
import { provideI18n } from '../i18n/provide-i18n';
import { NotificationBell } from './notification-bell';

/** SPEC-0004 BR2: shell-header bell — unread counter, hidden badge at 0, "99+" cap, and a link
 * to the notification center. */
describe('NotificationBell', () => {
  async function setup(unread: number) {
    const state = { unread: () => unread };
    TestBed.configureTestingModule({
      imports: [NotificationBell],
      providers: [provideRouter([]), provideI18n(), { provide: NotificationsStateService, useValue: state }],
    });
    const fixture = TestBed.createComponent(NotificationBell);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture };
  }

  it('shows no badge when there are no unread notifications', async () => {
    const { fixture } = await setup(0);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="notification-bell-count"]')).toBeNull();
  });

  it('shows the unread count in the badge (BR2)', async () => {
    const { fixture } = await setup(3);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="notification-bell-count"]')?.textContent?.trim()).toBe('3');
  });

  it('caps the badge at "99+" for very large counts', async () => {
    const { fixture } = await setup(150);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="notification-bell-count"]')?.textContent?.trim()).toBe('99+');
  });

  it('links to the notification center', async () => {
    const { fixture } = await setup(1);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="notification-bell"]')?.getAttribute('href')).toBe('/notificacoes');
  });
});
