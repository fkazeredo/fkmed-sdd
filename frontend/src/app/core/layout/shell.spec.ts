import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { provideI18n } from '../i18n/provide-i18n';
import { AuthService } from '../auth/auth.service';
import { AccessibleBeneficiary } from '../context/accessible-beneficiaries.api';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { NotificationsStateService } from '../notifications/notifications-state.service';
import { Shell } from './shell';

const MARIA: AccessibleBeneficiary = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' };

/** SPEC-0001 §Scope: shell with top bar and navigation placeholder, all strings pt-BR (BR7).
 * SPEC-0003 BR5: the active-beneficiary context loads on init; the selector itself is unit-tested
 * in isolation (`beneficiary-selector.spec.ts`), so it is faked here like AuthService.
 * SPEC-0004 BR2: the notification bell's unread count is likewise faked here — it is
 * unit-tested in isolation (`notification-bell.spec.ts`). */
describe('Shell', () => {
  const auth = {
    username: () => 'maria',
    logout: vi.fn(),
  };
  const context = {
    accessible: () => [MARIA],
    active: () => MARIA,
    load: vi.fn(),
    setActive: vi.fn(),
  };
  const notifications = {
    unread: () => 0,
    refreshUnread: vi.fn(),
  };

  beforeEach(async () => {
    context.load.mockClear();
    notifications.refreshUnread.mockClear();
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: AuthService, useValue: auth },
        { provide: BeneficiaryContextService, useValue: context },
        { provide: NotificationsStateService, useValue: notifications },
      ],
    }).compileComponents();
  });

  it('renders the brand, the logged user and the Meu Plano navigation in pt-BR', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="brand"]')?.textContent).toContain('FKMed');
    expect(el.querySelector('[data-testid="username"]')?.textContent).toContain('maria');
    expect(el.querySelector('[data-testid="nav-home"]')?.textContent).toContain('Início');
    expect(el.querySelector('[data-testid="nav-meu-plano"]')?.textContent).toContain('Meu Plano');
    expect(el.querySelector('[data-testid="nav-carteirinha"]')?.textContent).toContain('Carteirinha');
    expect(el.querySelector('[data-testid="nav-seguranca"]')?.textContent).toContain('Segurança');
    expect(el.textContent).toContain('Mais funcionalidades em breve');
    expect(el.textContent).toContain('Sair');
  });

  it('renders the notification bell in the header (SPEC-0004 BR2)', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="notification-bell"]'),
    ).not.toBeNull();
  });

  it('logs out through the AuthService', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const logoutButton = Array.from(el.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('Sair'),
    );
    logoutButton?.click();
    expect(auth.logout).toHaveBeenCalled();
  });

  it('loads the beneficiary context and the unread notification count on init (SPEC-0003 BR5, SPEC-0004 BR2)', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(context.load).toHaveBeenCalled();
    expect(notifications.refreshUnread).toHaveBeenCalled();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="active-beneficiary-name"]')?.textContent).toContain(
      'MARIA',
    );
  });
});
