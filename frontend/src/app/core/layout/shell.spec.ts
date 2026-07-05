import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { provideI18n } from '../i18n/provide-i18n';
import { AuthService } from '../auth/auth.service';
import { AccessibleBeneficiary } from '../context/accessible-beneficiaries.api';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { Shell } from './shell';

const MARIA: AccessibleBeneficiary = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' };

/** SPEC-0001 §Scope: shell with top bar and navigation placeholder, all strings pt-BR (BR7).
 * SPEC-0003 BR5: the active-beneficiary context loads on init; the selector itself is unit-tested
 * in isolation (`beneficiary-selector.spec.ts`), so it is faked here like AuthService. */
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

  beforeEach(async () => {
    context.load.mockClear();
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: AuthService, useValue: auth },
        { provide: BeneficiaryContextService, useValue: context },
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
    expect(el.querySelector('[data-testid="nav-seguranca"]')?.textContent).toContain('Segurança');
    expect(el.textContent).toContain('Mais funcionalidades em breve');
    expect(el.textContent).toContain('Sair');
  });

  it('logs out through the AuthService', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    expect(auth.logout).toHaveBeenCalled();
  });

  it('loads the beneficiary context on init and renders the active beneficiary (SPEC-0003 BR5)', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(context.load).toHaveBeenCalled();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="active-beneficiary-name"]')?.textContent).toContain(
      'MARIA',
    );
  });
});
