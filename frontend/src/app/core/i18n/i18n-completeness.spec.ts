import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { AuthService } from '../auth/auth.service';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { Shell } from '../layout/shell';
import { DigitalCard } from '../../features/card/digital-card';
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
import { PerfilMenu } from '../../features/perfil/perfil-menu';
import { AlterarFoto } from '../../features/perfil/alterar-foto';
import { AlterarCadastro } from '../../features/perfil/alterar-cadastro';
import { LegalDocumentPage } from '../../features/perfil/legal-document';
import { LegalAcceptance } from '../../features/perfil/legal-acceptance';
import { RedeHub } from '../../features/rede/rede-hub';
import { NetworkSearch } from '../../features/rede/network-search';
import { NetworkServiceType } from '../../features/rede/network-service-type';
import { NetworkSpecialty } from '../../features/rede/network-specialty';
import { NetworkResults } from '../../features/rede/network-results';
import { NetworkProviderDetail } from '../../features/rede/network-provider-detail';
import { NetworkFunnelState } from '../../features/rede/network-funnel-state.service';
import { AgendamentoHub } from '../../features/agendamento/agendamento-hub';
import { ConsultaWizard } from '../../features/agendamento/consulta-wizard';
import { ExameWizard } from '../../features/agendamento/exame-wizard';
import { MeusAgendamentos } from '../../features/agendamento/meus-agendamentos';
import { UnitPicker } from '../../features/agendamento/unit-picker';
import { SlotPicker } from '../../features/agendamento/slot-picker';
import { AppointmentView, AvailabilityDay } from '../../features/agendamento/appointments.api';
import { APP_VERSION } from '../config/app-version';
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
        DigitalCard,
        FirstAccess,
        EmailVerification,
        ForgotPassword,
        ResetPassword,
        Security,
        SessionExpired,
        NotificationCenter,
        NotificationPreferences,
        PerfilMenu,
        AlterarFoto,
        AlterarCadastro,
        LegalDocumentPage,
        LegalAcceptance,
        RedeHub,
        NetworkSearch,
        NetworkServiceType,
        NetworkSpecialty,
        NetworkResults,
        NetworkProviderDetail,
        AgendamentoHub,
        ConsultaWizard,
        ExameWizard,
        MeusAgendamentos,
        UnitPicker,
        SlotPicker,
      ],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: APP_VERSION, useValue: '0.0.0-test' },
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

    // Carteirinha (SPEC-0007): visual card + data sheet, "Salvar Carteirinha" (including its
    // inline PDF error), "Copiar número" confirmation, "Minhas Carteirinhas" (already exercises
    // both TITULAR/DEPENDENT role labels via the MARIA/PEDRO accessible list loaded above) and the
    // unavailable (BR10) / scope-denial (404) error states, reusing the same instance via retry().
    const card = TestBed.createComponent(DigitalCard);
    await card.whenStable();
    http.expectOne('/api/cards/pedro-id').flush({
      fullName: 'PEDRO SOUZA LIMA',
      cardNumber: '001234575',
      cns: '700000000000002',
      ansRegistration: '326305',
      coverage: 'ESTADUAL',
      planName: 'ADESÃO PRATA RJ QP COPART TP',
      planCategory: 'PRATA',
      additives: ['Urg/emerg Nacional Hr — Assistência'],
    });
    await card.whenStable();
    card.detectChanges();
    card.componentInstance.copied.set(true);
    card.detectChanges();
    card.componentInstance.downloading.set(true);
    card.detectChanges();
    card.componentInstance.downloading.set(false);
    card.componentInstance.pdfErrorKey.set('carteirinha.erro.pdf');
    card.detectChanges();

    card.componentInstance.retry();
    http
      .expectOne('/api/cards/pedro-id')
      .flush({ code: 'card.unavailable' }, { status: 409, statusText: 'Conflict' });
    await card.whenStable();
    card.detectChanges();

    card.componentInstance.retry();
    http
      .expectOne('/api/cards/pedro-id')
      .flush({ code: 'context.beneficiary-not-accessible' }, { status: 404, statusText: 'Not Found' });
    await card.whenStable();
    card.detectChanges();

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
    http.expectOne({ url: '/api/notifications/preferences', method: 'GET' }).flush({
      preferences: [
        { type: 'reimbursement.paid', description: 'Reembolso pago', emailOptOut: false, mandatory: false },
        { type: 'auth.password-changed', description: 'Senha alterada', emailOptOut: false, mandatory: true },
      ],
    });
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

  it('renders every SPEC-0006 Perfil screen without a missing translation', async () => {
    const handler = TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler;
    const http = TestBed.inject(HttpTestingController);
    // Seed the active-beneficiary context (real service) so the Perfil screens have an active id.
    TestBed.inject(BeneficiaryContextService).active.set({
      beneficiaryId: 'maria-id',
      firstName: 'MARIA',
      role: 'TITULAR',
    });

    // Perfil menu (BR1): header card, fixed-order items, version, LGPD accordion header and the
    // Sair confirmation dialog (opened so its keys render).
    const perfil = TestBed.createComponent(PerfilMenu);
    await perfil.whenStable();
    http.expectOne('/api/context/beneficiaries/maria-id').flush({
      firstName: 'MARIA',
      fullName: 'MARIA CLARA SOUZA LIMA',
      role: 'TITULAR',
      planName: 'PLANO MÉDICO — PRATA',
      cardNumber: '001234567',
      avatarUrl: null,
    });
    await perfil.whenStable();
    perfil.componentInstance.askLogout();
    perfil.detectChanges();

    // Alterar Foto: the current-photo blob-fetch (404 = no photo) + both success variants + error.
    const foto = TestBed.createComponent(AlterarFoto);
    await foto.whenStable();
    http
      .expectOne('/api/beneficiaries/maria-id/photo')
      .flush(null, { status: 404, statusText: 'Not Found' });
    await foto.whenStable();
    foto.componentInstance.errorKey.set('profile.photo-too-large');
    foto.componentInstance.success.set('saved');
    foto.detectChanges();
    foto.componentInstance.success.set('removed');
    foto.detectChanges();

    // Alterar Cadastro: contract read-only labels + every field-validation message + a server
    // field error + the success banner.
    const cadastro = TestBed.createComponent(AlterarCadastro);
    await cadastro.whenStable();
    http.expectOne('/api/beneficiaries/maria-id/profile').flush({
      fullName: 'MARIA CLARA SOUZA LIMA',
      cpf: '***.456.789-**',
      birthDate: '1990-05-10',
      cardNumber: '001234567',
      planName: 'PLANO MÉDICO',
      contactEmail: 'maria@fkmed.com',
      mobile: '(21) 99999-1234',
      landline: '',
      cep: '22222-222',
      street: 'Rua A',
      number: '10',
      complement: '',
      neighborhood: 'Centro',
      city: 'Rio de Janeiro',
      uf: 'RJ',
    });
    await cadastro.whenStable();
    cadastro.detectChanges();
    cadastro.componentInstance.form.contactEmail = '';
    cadastro.componentInstance.form.mobile = '';
    cadastro.componentInstance.form.landline = 'x';
    cadastro.componentInstance.form.cep = 'x';
    cadastro.componentInstance.form.uf = 'xxx';
    cadastro.componentInstance.errorField.set('cep');
    cadastro.componentInstance.errorKey.set('profile.cep-invalid');
    cadastro.componentInstance.success.set(true);
    cadastro.detectChanges();

    // Legal document page (TERMS by default) — fetches the text from GET /api/legal-documents/TERMS.
    const legalDoc = TestBed.createComponent(LegalDocumentPage);
    await legalDoc.whenStable();
    http.expectOne('/api/legal-documents/TERMS').flush({
      type: 'TERMS',
      version: '2.0',
      publishedAt: '2026-06-01',
      body: 'Texto dos termos.',
    });
    await legalDoc.whenStable();
    legalDoc.detectChanges();

    // Legal acceptance (interception screen): loads current (TERMS pending), then the pending
    // text; force the outdated-version message too.
    const aceite = TestBed.createComponent(LegalAcceptance);
    await aceite.whenStable();
    http.expectOne('/api/legal-documents/current').flush({
      terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false },
      privacy: { version: '1.0', publishedAt: '2026-01-01', acceptedByMe: true },
    });
    await aceite.whenStable();
    http.expectOne('/api/legal-documents/TERMS').flush({
      type: 'TERMS',
      version: '3.0',
      publishedAt: '2026-07-01',
      body: 'Novos termos.',
    });
    await aceite.whenStable();
    aceite.componentInstance.errorKey.set('legal.version-outdated');
    aceite.detectChanges();

    expect(
      Array.from(handler.missing),
      'SPEC-0006 Perfil UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });

  it('renders every SPEC-0008 Rede screen without a missing translation', () => {
    const handler = TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler;
    const http = TestBed.inject(HttpTestingController);
    const funnel = TestBed.inject(NetworkFunnelState);
    sessionStorage.removeItem('fkmed.networkFunnel');
    funnel.clear();

    // Rede hub: 4 cards, one enabled, three "em breve" — no HTTP calls.
    const hub = TestBed.createComponent(RedeHub);
    hub.detectChanges();

    // NetworkSearch: the locality funnel (State → Municipality → Neighborhood) and the name
    // search, including the optional municipality filter (only offered for a single-state
    // coverage) and the shared searchable-list's empty state.
    const search = TestBed.createComponent(NetworkSearch);
    search.detectChanges();
    // Real backend shape: raw arrays (no `{items:[…]}` envelope); states is UF codes only.
    http.expectOne('/api/network/states').flush(['RJ']);
    search.detectChanges();

    (search.nativeElement.querySelector('[data-testid="funil-uf"]') as HTMLElement).click();
    search.detectChanges();
    (search.nativeElement.querySelector('[data-testid="option-item-RJ"]') as HTMLElement).click();
    search.detectChanges();

    (search.nativeElement.querySelector('[data-testid="funil-municipio"]') as HTMLElement).click();
    search.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/municipalities')
      .flush(['Rio de Janeiro', 'Niterói']);
    search.detectChanges();
    const municipioInput = search.nativeElement.querySelector(
      '[data-testid="funil-municipio-dialog"] [data-testid="option-search-input"]',
    ) as HTMLInputElement;
    municipioInput.value = 'zzz';
    municipioInput.dispatchEvent(new Event('input'));
    search.detectChanges();
    http.expectOne((request) => request.url === '/api/network/municipalities').flush([]);
    search.detectChanges();
    municipioInput.value = '';
    municipioInput.dispatchEvent(new Event('input'));
    search.detectChanges();
    http.expectOne((request) => request.url === '/api/network/municipalities').flush(['Rio de Janeiro']);
    search.detectChanges();
    (search.nativeElement.querySelector('[data-testid="option-item-Rio de Janeiro"]') as HTMLElement).click();
    search.detectChanges();

    (search.nativeElement.querySelector('[data-testid="funil-bairro"]') as HTMLElement).click();
    search.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/neighborhoods')
      .flush(['Centro', 'Copacabana']);
    search.detectChanges();
    (search.nativeElement.querySelector('[data-testid="funil-bairro-todos"]') as HTMLElement).click();
    search.detectChanges();

    // Name search's optional municipality filter (only one covered state → available).
    (search.nativeElement.querySelector('[data-testid="nome-municipio-filtro"]') as HTMLElement)?.click();
    search.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/municipalities')
      .flush(['Rio de Janeiro']);
    search.detectChanges();
    (search.nativeElement.querySelector('[data-testid="option-item-Rio de Janeiro"]') as HTMLElement)?.click();
    search.detectChanges();

    // NetworkServiceType: locality summary (BR11) + the registry list (raw array + hasSpecialtyStep).
    funnel.setUf('RJ', 'Rio de Janeiro');
    funnel.setMunicipality('Rio de Janeiro');
    funnel.setNeighborhood('Centro');
    const serviceType = TestBed.createComponent(NetworkServiceType);
    serviceType.detectChanges();
    http
      .expectOne('/api/network/service-types')
      .flush([{ code: 'CONSULTORIOS', name: 'Consultórios–Clínicas–Terapias', hasSpecialtyStep: true }]);
    serviceType.detectChanges();

    // NetworkSpecialty: reached only for a type with a specialty step.
    funnel.setServiceType('CONSULTORIOS', 'Consultórios–Clínicas–Terapias', true);
    const specialty = TestBed.createComponent(NetworkSpecialty);
    specialty.detectChanges();
    http.expectOne('/api/network/specialties').flush([{ code: 'CARDIOLOGIA', name: 'Cardiologia' }]);
    specialty.detectChanges();

    // NetworkResults: empty state (BR10, funnel mode) — dataReferencia renders alongside it.
    funnel.setSpecialty('CARDIOLOGIA', 'Cardiologia');
    const resultsEmpty = TestBed.createComponent(NetworkResults);
    resultsEmpty.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/providers')
      .flush({ referenceDate: '2026-07-04', items: [] });
    resultsEmpty.detectChanges();

    // NetworkResults: the 422 network.query-too-short mapped message.
    const resultsError = TestBed.createComponent(NetworkResults);
    resultsError.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/providers')
      .flush({ code: 'network.query-too-short' }, { status: 422, statusText: 'Unprocessable Entity' });
    resultsError.detectChanges();

    // NetworkProviderDetail: full detail (seals, route/copy actions) + the unavailable state.
    const detail = TestBed.createComponent(NetworkProviderDetail);
    detail.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/providers/')
      .flush({
        id: 'p1',
        name: 'Dr. João Cardiologista',
        serviceType: 'Consultórios–Clínicas–Terapias',
        specialties: ['Cardiologia'],
        address: {
          cep: '20000-000',
          street: 'Rua das Flores',
          number: '10',
          complement: null,
          neighborhood: 'Centro',
          municipality: 'Rio de Janeiro',
          uf: 'RJ',
        },
        phone: '(21) 99999-0000',
        seals: [{ code: 'QUALI', name: 'Selo Qualidade', description: 'Excelente avaliação' }],
      });
    detail.detectChanges();
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    (detail.nativeElement.querySelector('[data-testid="detalhe-copiar-endereco"]') as HTMLElement).click();
    detail.detectChanges();

    const detailUnavailable = TestBed.createComponent(NetworkProviderDetail);
    detailUnavailable.detectChanges();
    http
      .expectOne((request) => request.url === '/api/network/providers/')
      .flush({ code: 'network.provider-unavailable' }, { status: 410, statusText: 'Gone' });
    detailUnavailable.detectChanges();

    expect(
      Array.from(handler.missing),
      'SPEC-0008 Rede UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });

  it('renders every SPEC-0009 Agendamento screen without a missing translation', async () => {
    const handler = TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler;
    const http = TestBed.inject(HttpTestingController);
    const context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' },
    ]);
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });

    const DAYS: AvailabilityDay[] = [
      {
        date: '2026-07-10',
        slots: [
          { slot: '2026-07-10T09:00', remaining: 2, available: true },
          { slot: '2026-07-10T09:30', remaining: 0, available: false },
        ],
      },
    ];
    const UNITS = [{ id: 'u1', name: 'Unidade Centro', address: 'Rua A, 10 — Centro' }];
    const el = (fixture: { nativeElement: HTMLElement }, testid: string): HTMLElement =>
      fixture.nativeElement.querySelector(`[data-testid="${testid}"]`) as HTMLElement;

    // Hub: 3 enabled cards + Telemedicina "em breve".
    const hub = TestBed.createComponent(AgendamentoHub);
    hub.detectChanges();

    // Empty/day states of the shared pickers (unidade.vazio, horario.vazio, escolhaDia/escolhaHora).
    const emptyUnits = TestBed.createComponent(UnitPicker);
    emptyUnits.componentRef.setInput('units', []);
    emptyUnits.detectChanges();
    const dayPicker = TestBed.createComponent(SlotPicker);
    dayPicker.componentRef.setInput('days', DAYS);
    dayPicker.detectChanges();
    (dayPicker.nativeElement.querySelector('[data-testid="slot-day-2026-07-10"]') as HTMLElement).click();
    dayPicker.detectChanges();
    const emptyDays = TestBed.createComponent(SlotPicker);
    emptyDays.componentRef.setInput('days', []);
    emptyDays.detectChanges();

    // Consultation wizard: gate → every step → review error variants → success.
    const consulta = TestBed.createComponent(ConsultaWizard);
    consulta.detectChanges();
    http.expectOne('/api/network/specialties').flush([{ code: 'CARDIOLOGIA', name: 'Cardiologia' }]);
    consulta.detectChanges();
    el(consulta, 'consulta-proximo').click(); // gate.especialidade
    consulta.detectChanges();
    consulta.componentInstance.selectSpecialty('CARDIOLOGIA');
    consulta.detectChanges();
    el(consulta, 'consulta-proximo').click(); // -> unit
    http.expectOne((r) => r.url === '/api/appointments/units').flush(UNITS);
    consulta.detectChanges();
    consulta.componentInstance.selectUnit('u1');
    consulta.detectChanges();
    el(consulta, 'consulta-proximo').click(); // -> slot
    http.expectOne((r) => r.url === '/api/appointments/availability').flush(DAYS);
    consulta.detectChanges();
    consulta.componentInstance['errorKey'].set('appointment.slot-taken'); // slot-taken banner
    consulta.detectChanges();
    (consulta.nativeElement.querySelector('[data-testid="slot-day-2026-07-10"]') as HTMLElement).click();
    consulta.detectChanges();
    consulta.componentInstance.selectSlot('2026-07-10T09:00');
    consulta.detectChanges();
    el(consulta, 'consulta-proximo').click(); // -> review
    consulta.detectChanges();
    consulta.componentInstance['errorKey'].set('appointment.time-conflict');
    consulta.detectChanges();
    consulta.componentInstance['errorKey'].set('appointment.outside-horizon');
    consulta.detectChanges();
    consulta.componentInstance['errorKey'].set(null);
    consulta.detectChanges();
    el(consulta, 'consulta-confirmar').click();
    http.expectOne({ url: '/api/appointments', method: 'POST' }).flush({ protocol: 'AG-20260704-0001', status: 'AGENDADO' });
    consulta.detectChanges();

    // Exam wizard: gate.exame/anexo, attachment name + remover + error keys, then success.
    const exame = TestBed.createComponent(ExameWizard);
    exame.detectChanges();
    http.expectOne('/api/appointments/exams').flush([{ code: 'HEMOGRAMA', name: 'Hemograma' }]);
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // gate.exame
    exame.detectChanges();
    exame.componentInstance.selectExam('HEMOGRAMA');
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // -> attachment
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // gate.anexo
    exame.detectChanges();
    await exame.componentInstance.acceptFile(
      new File([new Uint8Array([0x25, 0x50, 0x44, 0x46])], 'pedido.pdf', { type: 'application/pdf' }),
    );
    exame.detectChanges();
    exame.componentInstance['attachmentError'].set('appointment.attachment-invalid');
    exame.detectChanges();
    exame.componentInstance['attachmentError'].set('appointment.attachment-required');
    exame.detectChanges();
    exame.componentInstance['attachmentError'].set(null);
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // -> unit
    http.expectOne((r) => r.url === '/api/appointments/units').flush(UNITS);
    exame.detectChanges();
    exame.componentInstance.selectUnit('u1');
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // -> slot
    http.expectOne((r) => r.url === '/api/appointments/availability').flush(DAYS);
    exame.detectChanges();
    exame.componentInstance.selectSlot('2026-07-10T09:00');
    exame.detectChanges();
    el(exame, 'exame-proximo').click(); // -> review
    exame.detectChanges();
    el(exame, 'exame-confirmar').click();
    http.expectOne({ url: '/api/appointments', method: 'POST' }).flush({ protocol: 'AG-20260704-0002', status: 'AGENDADO' });
    exame.detectChanges();

    // Meus Agendamentos: loading → both tabs (all status labels + tipo + telemedicina badge) →
    // cancel dialog (too-late + not-found) → reschedule dialog (slot-taken) → empty + error states.
    const UPCOMING: AppointmentView[] = [
      {
        id: 'a1', protocol: 'AG-1', type: 'CONSULTATION', specialtyCode: 'CARDIOLOGIA',
        specialtyName: 'Cardiologia', examCode: null, examName: null, beneficiaryId: 'maria-id',
        beneficiaryName: 'MARIA', unitId: 'u1', unitName: 'Unidade Centro',
        scheduledAt: '2026-07-10T09:00', status: 'AGENDADO', telemedicine: true,
      },
      {
        id: 'a2', protocol: 'AG-2', type: 'EXAM', specialtyCode: null, examCode: 'HEMOGRAMA',
        examName: 'Hemograma', beneficiaryId: 'pedro-id', beneficiaryName: 'PEDRO', unitId: 'u2',
        unitName: 'Unidade Tijuca', scheduledAt: '2026-07-12T10:00', status: 'REAGENDADO',
      },
    ];
    const HISTORY: AppointmentView[] = [
      {
        id: 'a4', protocol: 'AG-4', type: 'CONSULTATION', specialtyCode: 'ORTOPEDIA',
        specialtyName: 'Ortopedia', examCode: null, examName: null, beneficiaryId: 'pedro-id',
        beneficiaryName: 'PEDRO', unitId: 'u1', unitName: 'Unidade Centro',
        scheduledAt: '2026-06-25T14:00', status: 'REALIZADO',
      },
      {
        id: 'a3', protocol: 'AG-3', type: 'CONSULTATION', specialtyCode: 'DERMATOLOGIA',
        specialtyName: 'Dermatologia', examCode: null, examName: null, beneficiaryId: 'maria-id',
        beneficiaryName: 'MARIA', unitId: 'u1', unitName: 'Unidade Centro',
        scheduledAt: '2026-06-20T10:00', status: 'CANCELADO',
      },
    ];
    const meus = TestBed.createComponent(MeusAgendamentos);
    meus.detectChanges(); // loading (common.loading)
    http.expectOne((r) => r.url === '/api/appointments').flush({ upcoming: UPCOMING, history: HISTORY });
    meus.detectChanges();
    el(meus, 'meus-tab-historico').click();
    meus.detectChanges();
    el(meus, 'meus-tab-proximos').click();
    meus.detectChanges();
    meus.componentInstance.askCancel(UPCOMING[0]);
    meus.detectChanges();
    meus.componentInstance['cancelError'].set('appointment.cancel-too-late');
    meus.detectChanges();
    meus.componentInstance['cancelError'].set('appointment.not-found');
    meus.detectChanges();
    meus.componentInstance.closeCancel();
    meus.detectChanges();
    meus.componentInstance.askReschedule(UPCOMING[1]);
    http.expectOne((r) => r.url === '/api/appointments/availability').flush(DAYS);
    meus.detectChanges();
    meus.componentInstance['rescheduleError'].set('appointment.slot-taken');
    meus.detectChanges();
    meus.componentInstance.closeReschedule();
    meus.detectChanges();
    // Empty state.
    meus.componentInstance.load();
    http.expectOne((r) => r.url === '/api/appointments').flush({ upcoming: [], history: [] });
    meus.detectChanges();
    // Error state (common.error + retry).
    meus.componentInstance.load();
    http
      .expectOne((r) => r.url === '/api/appointments')
      .flush({ code: 'x' }, { status: 500, statusText: 'Server Error' });
    meus.detectChanges();

    expect(
      Array.from(handler.missing),
      'SPEC-0009 Agendamento UI keys missing from the pt-BR bundle',
    ).toHaveLength(0);
  });
});
