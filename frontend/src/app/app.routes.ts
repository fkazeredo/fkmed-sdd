import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
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
