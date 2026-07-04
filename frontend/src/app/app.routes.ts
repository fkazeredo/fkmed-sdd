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
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'meu-plano' },
      {
        path: 'meu-plano',
        loadComponent: () => import('./features/my-plan/my-plan').then((m) => m.MyPlan),
      },
    ],
  },
];
