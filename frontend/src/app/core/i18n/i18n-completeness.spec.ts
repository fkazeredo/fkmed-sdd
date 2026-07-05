import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { AuthService } from '../auth/auth.service';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { Shell } from '../layout/shell';
import { MyPlan } from '../../features/my-plan/my-plan';
import { FirstAccess } from '../../features/first-access/first-access';
import { EmailVerification } from '../../features/email-verification/email-verification';
import { ForgotPassword } from '../../features/password-recovery/forgot-password';
import { ResetPassword } from '../../features/password-recovery/reset-password';
import { Security } from '../../features/security/security';
import { SessionExpired } from '../../features/session-expired/session-expired';
import { provideI18n, ReportMissingTranslationHandler } from './provide-i18n';
import { TRANSLATIONS } from './translations';

/**
 * SPEC-0001 AC5 / SPEC-0002 BR16: every visible UI string of the slice resolves from the pt-BR
 * bundle. Renders every screen of the slice — including all branches of the first-access,
 * verification, password-recovery, Segurança and session-expiry flows, and the SPEC-0003
 * active-beneficiary selector (both TITULAR and DEPENDENT role labels) embedded in the shell —
 * with a recording MissingTranslationHandler; a single missing key fails.
 */
describe('i18n completeness (pt-BR)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        Shell,
        MyPlan,
        FirstAccess,
        EmailVerification,
        ForgotPassword,
        ResetPassword,
        Security,
        SessionExpired,
      ],
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
    // SPEC-0003 BR5: the shell loads the active-beneficiary context on init — flush both a
    // TITULAR and a DEPENDENT beneficiary, then switch the active one, to exercise both role
    // labels (contexto.papel.TITULAR / contexto.papel.DEPENDENT) through the selector.
    http.expectOne('/api/context/accessible-beneficiaries').flush([
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' },
    ]);
    await shell.whenStable();
    shell.detectChanges();
    TestBed.inject(BeneficiaryContextService).setActive('pedro-id');
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

    // Esqueci minha senha: form + validation message, then the neutral confirmation (BR7).
    const forgotPassword = TestBed.createComponent(ForgotPassword);
    forgotPassword.detectChanges();
    forgotPassword.componentInstance.email = 'invalido';
    forgotPassword.detectChanges();
    forgotPassword.componentInstance.done.set(true);
    forgotPassword.detectChanges();

    // Redefinir senha: no token → invalid by default; force the form, error and success states.
    const resetPassword = TestBed.createComponent(ResetPassword);
    resetPassword.detectChanges();
    expect(resetPassword.componentInstance.status()).toBe('invalid');
    resetPassword.componentInstance.status.set('form');
    resetPassword.componentInstance.newPassword = 'x';
    resetPassword.componentInstance.confirmPassword = 'y';
    resetPassword.componentInstance.togglePassword();
    resetPassword.componentInstance.errorKey.set('redefinirSenha.erro.senhaFraca');
    resetPassword.detectChanges();
    resetPassword.componentInstance.status.set('success');
    resetPassword.detectChanges();

    // Segurança: validation messages, both inline error fields and the success banner.
    const security = TestBed.createComponent(Security);
    security.detectChanges();
    security.componentInstance.newPassword = 'x';
    security.componentInstance.confirmPassword = 'y';
    security.componentInstance.toggleCurrent();
    security.componentInstance.toggleNew();
    security.detectChanges();
    security.componentInstance.errorKey.set('seguranca.erro.senhaAtualIncorreta');
    security.componentInstance.errorField.set('currentPassword');
    security.detectChanges();
    security.componentInstance.errorKey.set('seguranca.erro.senhaFraca');
    security.componentInstance.errorField.set('newPassword');
    security.detectChanges();
    security.componentInstance.errorField.set(null);
    security.componentInstance.success.set(true);
    security.detectChanges();

    // Sessão expirada: static notice.
    const sessionExpired = TestBed.createComponent(SessionExpired);
    sessionExpired.detectChanges();

    expect(
      Array.from(handler.missing),
      'UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });
});
