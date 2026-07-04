import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { AuthService } from '../auth/auth.service';
import { Shell } from '../layout/shell';
import { MyPlan } from '../../features/my-plan/my-plan';
import { FirstAccess } from '../../features/first-access/first-access';
import { EmailVerification } from '../../features/email-verification/email-verification';
import { provideI18n, ReportMissingTranslationHandler } from './provide-i18n';
import { TRANSLATIONS } from './translations';

/**
 * SPEC-0001 AC5 / SPEC-0002 BR16: every visible UI string of the slice resolves from the pt-BR
 * bundle. Renders every screen of the slice — including all branches of the first-access and
 * verification flows — with a recording MissingTranslationHandler; a single missing key fails.
 */
describe('i18n completeness (pt-BR)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell, MyPlan, FirstAccess, EmailVerification],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        {
          provide: AuthService,
          useValue: { username: () => 'maria', logout: vi.fn(), login: vi.fn() },
        },
      ],
    }).compileComponents();
  });

  it('has no blank message in the bundle', () => {
    const bundle = TRANSLATIONS['pt-BR'];
    expect(Object.keys(bundle).length).toBeGreaterThan(0);
    for (const [key, value] of Object.entries(bundle)) {
      expect(value, `key '${key}' must have a pt-BR message`).toBeTruthy();
    }
  });

  it('renders every screen of the slice without a missing translation', async () => {
    const handler = TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler;
    const http = TestBed.inject(HttpTestingController);

    const shell = TestBed.createComponent(Shell);
    await shell.whenStable();
    shell.detectChanges();

    const myPlan = TestBed.createComponent(MyPlan);
    await myPlan.whenStable();
    http.expectOne('/api/plan/my-plan').flush({
      plan: {
        name: 'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
        ansRegistration: '326305',
        coverage: 'ESTADUAL',
        copay: true,
        reimbursement: false,
        additives: ['Urg/emerg Nacional Hr — Assistência'],
      },
      members: [
        { fullName: 'MARIA CLARA SOUZA LIMA', role: 'TITULAR', cardNumber: '001234567' },
        { fullName: 'PEDRO SOUZA LIMA', role: 'DEPENDENT', cardNumber: '001234575' },
      ],
    });
    await myPlan.whenStable();
    myPlan.detectChanges();

    // First-access wizard: exercise every step, the field validations and the error block.
    const firstAccess = TestBed.createComponent(FirstAccess);
    await firstAccess.whenStable();
    firstAccess.componentInstance.cpf = '1';
    firstAccess.componentInstance.cardNumber = '1';
    firstAccess.detectChanges();
    firstAccess.componentInstance.step.set(2);
    firstAccess.componentInstance.email = 'bad';
    firstAccess.componentInstance.password = 'x';
    firstAccess.detectChanges();
    firstAccess.componentInstance.step.set(3);
    firstAccess.detectChanges();
    firstAccess.componentInstance.errorKey.set('primeiroAcesso.erro.jaExiste');
    firstAccess.detectChanges();

    // Verification landing: idle→resend, confirmed and invalid branches.
    const verification = TestBed.createComponent(EmailVerification);
    await verification.whenStable();
    verification.detectChanges();
    verification.componentInstance.status.set('confirmed');
    verification.detectChanges();
    verification.componentInstance.status.set('invalid');
    verification.componentInstance.resendDone.set(true);
    verification.detectChanges();

    expect(
      Array.from(handler.missing),
      'UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });
});
