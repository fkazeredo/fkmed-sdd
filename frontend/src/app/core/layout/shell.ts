import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../auth/auth.service';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { BeneficiarySelector } from './beneficiary-selector';

/**
 * App shell (SPEC-0001 §Scope): top bar (with the SPEC-0003 active-beneficiary selector), main
 * navigation placeholder and content outlet. Only mounted behind `authGuard` (app.routes.ts), so
 * loading the beneficiary context here always runs for an authenticated user.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe, ButtonModule, BeneficiarySelector],
  templateUrl: './shell.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly context = inject(BeneficiaryContextService);

  ngOnInit(): void {
    this.context.load();
  }

  logout(): void {
    this.auth.logout();
  }
}
