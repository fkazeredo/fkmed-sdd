import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { legalAcceptanceGuard } from './core/legal/legal-acceptance.guard';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
  // Public routes (SPEC-0002): no auth guard, no shell — a visitor reaches these before login.
  {
    path: 'primeiro-acesso',
    loadComponent: () => import('./features/first-access/first-access').then((m) => m.FirstAccess),
  },
  {
    path: 'verificar-email',
    loadComponent: () =>
      import('./features/email-verification/email-verification').then((m) => m.EmailVerification),
  },
  // BR10 password recovery: "Esqueci minha senha" (request) and "Redefinir senha" (reached from
  // the e-mailed link — /redefinir-senha?token=..., PasswordRecoveryEmailListener on the backend).
  {
    path: 'recuperar-senha',
    loadComponent: () =>
      import('./features/password-recovery/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'redefinir-senha',
    loadComponent: () =>
      import('./features/password-recovery/reset-password').then((m) => m.ResetPassword),
  },
  // BR12: the session-expiry interceptor sends the (now unauthenticated) user here.
  {
    path: 'sessao-expirada',
    loadComponent: () =>
      import('./features/session-expired/session-expired').then((m) => m.SessionExpired),
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    // SPEC-0006 BR8: terms interception. While a new mandatory legal version is unaccepted, this
    // guard blocks every internal route and funnels the user to /aceite-legal (only Sair, the
    // header logout, stays reachable) until "Li e aceito".
    canActivateChild: [legalAcceptanceGuard],
    children: [
      // SPEC-0005: Home is the new default child — the daily entry point after login. Meu Plano
      // remains reachable through the shell nav (nav-meu-plano).
      { path: '', pathMatch: 'full', redirectTo: 'home' },
      {
        path: 'home',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'meu-plano',
        loadComponent: () => import('./features/my-plan/my-plan').then((m) => m.MyPlan),
      },
      // SPEC-0007 (Phase 2): the Digital Card screen — reachable via the shell nav
      // (nav-carteirinha); the Home "Acesso Rápido" shortcut for it stays disabled/"em breve"
      // for now (SPEC-0005 phased-delivery note is Home's own scope, not touched here).
      {
        path: 'carteirinha',
        loadComponent: () => import('./features/card/digital-card').then((m) => m.DigitalCard),
      },
      // Segurança (SPEC-0002 AC-8): reachable via a direct route for now — the full Perfil menu
      // is SPEC-0006.
      {
        path: 'seguranca',
        loadComponent: () => import('./features/security/security').then((m) => m.Security),
      },
      // SPEC-0004: notification center (reached via the shell bell, not the main nav) and its
      // preferences screen (reached from a link inside the center).
      {
        path: 'notificacoes',
        loadComponent: () =>
          import('./features/notifications/notification-center').then((m) => m.NotificationCenter),
      },
      {
        path: 'notificacoes/preferencias',
        loadComponent: () =>
          import('./features/notifications/notification-preferences').then(
            (m) => m.NotificationPreferences,
          ),
      },
      // SPEC-0006 (Phase 2): Perfil menu (BR1) and its sub-screens.
      {
        path: 'perfil',
        loadComponent: () => import('./features/perfil/perfil-menu').then((m) => m.PerfilMenu),
      },
      {
        path: 'perfil/foto',
        loadComponent: () => import('./features/perfil/alterar-foto').then((m) => m.AlterarFoto),
      },
      {
        path: 'perfil/cadastro',
        loadComponent: () =>
          import('./features/perfil/alterar-cadastro').then((m) => m.AlterarCadastro),
      },
      // Legal pages (BR8): reached from the Perfil menu, shown with version + publication date.
      {
        path: 'termos-de-uso',
        data: { type: 'TERMS' },
        loadComponent: () => import('./features/perfil/legal-document').then((m) => m.LegalDocumentPage),
      },
      {
        path: 'comunicado-privacidade',
        data: { type: 'PRIVACY' },
        loadComponent: () => import('./features/perfil/legal-document').then((m) => m.LegalDocumentPage),
      },
      // Legal acceptance interception target (BR8): the guard redirects here while a version is
      // pending; it self-redirects to /home once nothing is pending.
      {
        path: 'aceite-legal',
        loadComponent: () => import('./features/perfil/legal-acceptance').then((m) => m.LegalAcceptance),
      },
      // SPEC-0008 (Phase 3): the Rede hub and the Provider Network Search journey — reachable via
      // the shell nav (nav-rede). Only "Busca de rede" is built in this phase; the hub's other 3
      // cards (Agendamento/Telemedicina/Minha Saúde) render disabled until their specs land.
      {
        path: 'rede',
        loadComponent: () => import('./features/rede/rede-hub').then((m) => m.RedeHub),
      },
      {
        path: 'rede/busca',
        loadComponent: () => import('./features/rede/network-search').then((m) => m.NetworkSearch),
      },
      {
        path: 'rede/busca/tipo-servico',
        loadComponent: () =>
          import('./features/rede/network-service-type').then((m) => m.NetworkServiceType),
      },
      {
        path: 'rede/busca/especialidade',
        loadComponent: () => import('./features/rede/network-specialty').then((m) => m.NetworkSpecialty),
      },
      {
        path: 'rede/busca/resultados',
        loadComponent: () => import('./features/rede/network-results').then((m) => m.NetworkResults),
      },
      {
        path: 'rede/busca/prestador/:id',
        loadComponent: () =>
          import('./features/rede/network-provider-detail').then((m) => m.NetworkProviderDetail),
      },
      // SPEC-0009 (Phase 3): the Agendamento hub and its scheduling journeys — reachable via the
      // shell nav (nav-agendamento). Consultation/exam wizards, Meus Agendamentos; Telemedicina
      // booking stays "em breve" (SPEC-0010).
      {
        path: 'agendamento',
        loadComponent: () =>
          import('./features/agendamento/agendamento-hub').then((m) => m.AgendamentoHub),
      },
      {
        path: 'agendamento/consulta',
        loadComponent: () =>
          import('./features/agendamento/consulta-wizard').then((m) => m.ConsultaWizard),
      },
      {
        path: 'agendamento/exame',
        loadComponent: () =>
          import('./features/agendamento/exame-wizard').then((m) => m.ExameWizard),
      },
      {
        path: 'agendamento/meus',
        loadComponent: () =>
          import('./features/agendamento/meus-agendamentos').then((m) => m.MeusAgendamentos),
      },
      // SPEC-0011 (Phase 4): the Minha Saúde hub and its 3 clinical-document categories —
      // reachable via the shell nav (nav-minha-saude). "Receituários/Atestados" covers 2 wire
      // categories (PRESCRIPTION + SICK_NOTE, BR1) merged client-side (DocumentList).
      {
        path: 'minha-saude',
        loadComponent: () => import('./features/minha-saude/minha-saude-hub').then((m) => m.MinhaSaudeHub),
      },
      {
        path: 'minha-saude/exames',
        data: { categories: ['EXAM_ORDER'], titleKey: 'minhaSaude.exames.title' },
        loadComponent: () => import('./features/minha-saude/document-list').then((m) => m.DocumentList),
      },
      {
        path: 'minha-saude/encaminhamentos',
        data: { categories: ['REFERRAL'], titleKey: 'minhaSaude.encaminhamentos.title' },
        loadComponent: () => import('./features/minha-saude/document-list').then((m) => m.DocumentList),
      },
      {
        path: 'minha-saude/receituarios',
        data: { categories: ['PRESCRIPTION', 'SICK_NOTE'], titleKey: 'minhaSaude.receituarios.title' },
        loadComponent: () => import('./features/minha-saude/document-list').then((m) => m.DocumentList),
      },
      {
        path: 'minha-saude/documento/:id',
        loadComponent: () => import('./features/minha-saude/document-detail').then((m) => m.DocumentDetail),
      },
      // SPEC-0010 (Phase 4): the Telemedicina hub and its journeys — reachable via the shell nav
      // (nav-telemedicina). Pronto Atendimento (triage → term → queue → room → summary, state-driven
      // per ADR-0015, live via SSE per ADR-0016), scheduled teleconsultation and the tele-filtered
      // Meus Agendamentos with the join window.
      {
        path: 'telemedicina',
        loadComponent: () =>
          import('./features/telemedicina/telemedicina-hub').then((m) => m.TelemedicinaHub),
      },
      {
        path: 'telemedicina/triagem',
        loadComponent: () =>
          import('./features/telemedicina/pronto-atendimento').then((m) => m.ProntoAtendimento),
      },
      {
        path: 'telemedicina/sessao',
        loadComponent: () =>
          import('./features/telemedicina/sessao-atendimento').then((m) => m.SessaoAtendimento),
      },
      {
        path: 'telemedicina/agendar',
        loadComponent: () =>
          import('./features/telemedicina/agendar-teleconsulta').then((m) => m.AgendarTeleconsulta),
      },
      {
        path: 'telemedicina/agendamentos',
        loadComponent: () =>
          import('./features/telemedicina/meus-agendamentos-tele').then((m) => m.MeusAgendamentosTele),
      },
      // SPEC-0012 (Phase 5): Guias e Token — reachable via the shell nav (nav-guias). One hub
      // screen (guides list + token, BR1) plus the guide detail.
      {
        path: 'guias',
        loadComponent: () => import('./features/guias/guias-hub').then((m) => m.GuiasHub),
      },
      {
        path: 'guias/:id',
        loadComponent: () => import('./features/guias/guide-detail').then((m) => m.GuideDetail),
      },
      // SPEC-0013 (Phase 5): Finanças — reachable via the shell nav (nav-financas, hidden for
      // dependents). Titular-only (BR1): every screen renders a friendly denial when the active
      // beneficiary is a dependent. Hub (invoice tabs) + boleto detail + validator + copay + IR +
      // Lei 12.007 settlement.
      {
        path: 'financas',
        loadComponent: () => import('./features/financas/financas-hub').then((m) => m.FinancasHub),
      },
      {
        path: 'financas/boleto/:id',
        loadComponent: () => import('./features/financas/boleto-detail').then((m) => m.BoletoDetail),
      },
      {
        path: 'financas/validar',
        loadComponent: () =>
          import('./features/financas/validar-boleto').then((m) => m.ValidarBoleto),
      },
      {
        path: 'financas/coparticipacao',
        loadComponent: () =>
          import('./features/financas/coparticipacao').then((m) => m.Coparticipacao),
      },
      {
        path: 'financas/imposto-renda',
        loadComponent: () =>
          import('./features/financas/imposto-renda').then((m) => m.ImpostoRenda),
      },
      {
        path: 'financas/quitacao',
        loadComponent: () => import('./features/financas/quitacao').then((m) => m.Quitacao),
      },
      // SPEC-0014 (Phase 5, closes it): Atendimento — channel cards + antifraude section (anchor
      // target of the Home fraud banner, AC2), FAQ (search + category, AC1/AC3) and Central de
      // Libras (AC4). Reachable via the shell nav (nav-atendimento).
      {
        path: 'atendimento',
        loadComponent: () =>
          import('./features/atendimento/atendimento-hub').then((m) => m.AtendimentoHub),
      },
      {
        path: 'atendimento/faq',
        loadComponent: () => import('./features/atendimento/faq').then((m) => m.Faq),
      },
      {
        path: 'atendimento/libras',
        loadComponent: () => import('./features/atendimento/libras').then((m) => m.Libras),
      },
    ],
  },
];
