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
    ],
  },
];
