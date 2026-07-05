import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { AuthService } from '../auth/auth.service';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { Shell } from '../layout/shell';
import { Home } from '../../features/home/home';
import { MyPlan } from '../../features/my-plan/my-plan';
import { FirstAccess } from '../../features/first-access/first-access';
import { EmailVerification } from '../../features/email-verification/email-verification';
import { ForgotPassword } from '../../features/password-recovery/forgot-password';
import { ResetPassword } from '../../features/password-recovery/reset-password';
import { Security } from '../../features/security/security';
import { SessionExpired } from '../../features/session-expired/session-expired';
import { NotificationCenter } from '../../features/notifications/notification-center';
import { NotificationPreferences } from '../../features/notifications/notification-preferences';
import { provideI18n, ReportMissingTranslationHandler } from './provide-i18n';
import { TRANSLATIONS } from './translations';

/**
 * SPEC-0001 AC5 / SPEC-0002 BR16: every visible UI string of the slice resolves from the pt-BR
 * bundle. Renders every screen of the slice — including all branches of the first-access,
 * verification, password-recovery, Segurança and session-expiry flows, the SPEC-0003
 * active-beneficiary selector (both TITULAR and DEPENDENT role labels) embedded in the shell,
 * and the SPEC-0004 notification bell/center/preferences — with a recording
 * MissingTranslationHandler; a single missing key fails.
 */
describe('i18n completeness (pt-BR)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        Shell,
        Home,
        MyPlan,
        FirstAccess,
        EmailVerification,
        ForgotPassword,
        ResetPassword,
        Security,
        SessionExpired,
        NotificationCenter,
        NotificationPreferences,
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
    // SPEC-0004 BR2: the shell also loads the unread notification count on init (the bell).
    http
      .expectOne((request) => request.url === '/api/notifications' && request.params.get('size') === '1')
      .flush({ unread: 2, items: [] });
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

    // Home (SPEC-0005): the shell section above already switched the active beneficiary to
    // PEDRO, so the card's effect fires against his id straight away. Exercises the card, both
    // "em breve" dialog triggers (avatar / Reconhecimento Facial), the disabled quick-access
    // hints, the banners (rendered + disabled CTA) and the notices accordion (default-open +
    // ALERT tag + switching to the other panel).
    const home = TestBed.createComponent(Home);
    await home.whenStable();
    http.expectOne('/api/context/beneficiaries/pedro-id').flush({
      firstName: 'PEDRO',
      fullName: 'PEDRO SOUZA LIMA',
      role: 'DEPENDENT',
      planName: 'ADESÃO PRATA RJ QP COPART TP',
      cardNumber: '001234575',
      avatarUrl: null,
    });
    http.expectOne('/api/content/home').flush({
      banners: [
        {
          title: 'Alerta de golpe!',
          text: 'A operadora não solicita dados ou pagamentos por WhatsApp.',
          buttonLabel: 'Saiba mais',
          destination: '/atendimento#antifraude',
          imageUrl: null,
          order: 1,
        },
      ],
      notices: [
        {
          title: 'Instabilidade momentânea da Telemedicina',
          severity: 'ALERT',
          body: 'Estamos normalizando o serviço de Telemedicina.',
          order: 1,
        },
        {
          title: 'Lei Geral de Proteção de Dados Pessoais',
          severity: 'INFORMATIVE',
          body: 'Saiba como tratamos seus dados pessoais.',
          order: 2,
        },
      ],
    });
    await home.whenStable();
    home.detectChanges();

    (home.nativeElement.querySelector('[data-testid="card-avatar"]') as HTMLElement).click();
    home.detectChanges();
    home.componentInstance.closeDialog();
    home.detectChanges();
    (home.nativeElement.querySelector('[data-testid="shortcut-reconhecimentoFacial"]') as HTMLElement).click();
    home.detectChanges();
    home.componentInstance.closeDialog();
    home.detectChanges();
    (home.nativeElement.querySelector('[data-testid="notice-2"] p-accordion-header') as HTMLElement).click();
    home.detectChanges();

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

    // Notification center (SPEC-0004): the loaded list — an unread item with a deep link, an
    // already-read item without one — exercising the title, item content, "marcar como lida",
    // "marcar todas como lidas" and the preferences link; then mark-all-read.
    const notificationCenter = TestBed.createComponent(NotificationCenter);
    await notificationCenter.whenStable();
    http
      .expectOne((request) => request.url === '/api/notifications' && request.params.get('page') === '0')
      .flush({
        unread: 1,
        items: [
          {
            id: 'notif-1',
            type: 'reimbursement.paid',
            title: 'Reembolso pago',
            body: 'Seu reembolso RE-20260601-0001 foi pago: R$ 120,00.',
            link: '/reembolso/RE-20260601-0001',
            createdAt: '2026-07-01T10:00:00Z',
            read: false,
          },
          {
            id: 'notif-2',
            type: 'guide.status-changed',
            title: 'Guia atualizada',
            body: 'Sua guia teve o status atualizado.',
            link: null,
            createdAt: '2026-06-30T09:00:00Z',
            read: true,
          },
        ],
      });
    await notificationCenter.whenStable();
    notificationCenter.detectChanges();
    (
      notificationCenter.nativeElement.querySelector('[data-testid="notifications-mark-all"]') as HTMLElement
    ).click();
    http.expectOne({ url: '/api/notifications/read-all', method: 'POST' }).flush(null);
    await notificationCenter.whenStable();
    notificationCenter.detectChanges();

    // Notification preferences (SPEC-0004 BR7): a mandatory (locked) and an optional type —
    // toggle the optional one to exercise both the "ativado"/"desativado" labels, then force the
    // defensive mandatory-refusal error banner.
    const notificationPreferences = TestBed.createComponent(NotificationPreferences);
    await notificationPreferences.whenStable();
    http.expectOne({ url: '/api/notifications/preferences', method: 'GET' }).flush([
      { code: 'reimbursement.paid', description: 'Reembolso pago', emailOptOut: false, mandatory: false },
      { code: 'auth.password-changed', description: 'Senha alterada', emailOptOut: false, mandatory: true },
    ]);
    await notificationPreferences.whenStable();
    notificationPreferences.detectChanges();
    (
      notificationPreferences.nativeElement.querySelector(
        '[data-testid="preference-reimbursement.paid-toggle"]',
      ) as HTMLElement
    ).click();
    http
      .expectOne({ url: '/api/notifications/preferences', method: 'PUT' })
      .flush({ code: 'notification.preference-mandatory' }, { status: 422, statusText: 'Unprocessable Entity' });
    await notificationPreferences.whenStable();
    notificationPreferences.detectChanges();

    // Sessão expirada: static notice.
    const sessionExpired = TestBed.createComponent(SessionExpired);
    sessionExpired.detectChanges();

    expect(
      Array.from(handler.missing),
      'UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });
});
