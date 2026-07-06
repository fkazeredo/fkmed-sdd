import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { APP_VERSION } from '../../core/config/app-version';
import { AvatarStateService } from '../../core/context/avatar-state.service';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummary } from '../../core/context/beneficiary-summary.api';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { PerfilMenu } from './perfil-menu';

const ACTIVE = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const };

const SUMMARY: BeneficiarySummary = {
  firstName: 'MARIA',
  fullName: 'MARIA CLARA SOUZA LIMA',
  role: 'TITULAR',
  planName: 'PLANO MÉDICO — PRATA',
  cardNumber: '001234567',
  avatarUrl: null,
};

const EXPECTED_ORDER = [
  'perfil-item-alterar-foto',
  'perfil-item-seguranca',
  'perfil-item-alterar-cadastro',
  'perfil-item-libras',
  'perfil-item-faq',
  'perfil-item-privacidade',
  'perfil-item-termos',
  'perfil-item-sair',
];

describe('PerfilMenu (SPEC-0006 BR1/BR9/BR10)', () => {
  let http: HttpTestingController;
  const auth = { logout: vi.fn() };

  beforeEach(async () => {
    auth.logout.mockClear();
    URL.createObjectURL = vi.fn(() => 'blob:mock');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [PerfilMenu],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        provideRouter([]),
        { provide: BeneficiaryContextService, useValue: { active: signal(ACTIVE) } },
        { provide: AuthService, useValue: auth },
        { provide: APP_VERSION, useValue: '9.9.9' },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function create() {
    const fixture = TestBed.createComponent(PerfilMenu);
    await fixture.whenStable();
    http.expectOne('/api/context/beneficiaries/maria-id').flush(SUMMARY);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('renders the header card with greeting, plan and card number (BR1)', async () => {
    const fixture = await create();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="perfil-saudacao"]')?.textContent).toContain('MARIA');
    expect(el.querySelector('[data-testid="perfil-plano"]')?.textContent).toContain('PLANO MÉDICO — PRATA');
    expect(el.querySelector('[data-testid="perfil-carteirinha"]')?.textContent).toContain('001234567');
  });

  it('lists the menu items in the fixed order (BR1)', async () => {
    const fixture = await create();
    const items = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('[data-testid^="perfil-item-"]'),
    ).map((node) => node.getAttribute('data-testid'));
    expect(items).toEqual(EXPECTED_ORDER);
  });

  it('shows the build product version right by Sair (BR10)', async () => {
    const fixture = await create();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="perfil-versao"]')?.textContent).toContain(
      '9.9.9',
    );
  });

  it('Sair asks for confirmation and only then ends the session (BR9)', async () => {
    const fixture = await create();
    const el = fixture.nativeElement as HTMLElement;
    (el.querySelector('[data-testid="perfil-item-sair"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(fixture.componentInstance.confirmingLogout()).toBe(true);
    // Confirming logs out; cancelling would not.
    expect(auth.logout).not.toHaveBeenCalled();
    fixture.componentInstance.confirmLogout();
    expect(auth.logout).toHaveBeenCalled();
  });

  it('reflects an avatar change from the shared state without reload (BR3)', async () => {
    const fixture = await create();
    expect(fixture.componentInstance.avatarUrl()).toBeNull();
    TestBed.inject(AvatarStateService).setFromBlob('maria-id', new Blob(['x']));
    expect(fixture.componentInstance.avatarUrl()).toBe('blob:mock');
  });
});
