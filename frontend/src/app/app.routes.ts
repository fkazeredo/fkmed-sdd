import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
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
    ],
  },
];
