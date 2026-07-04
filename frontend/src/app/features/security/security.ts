import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { differsFromEmail, meetsPasswordPolicy } from '../../shared/validators/password-policy';
import { SecurityApi } from './security.api';

type ErrorField = 'currentPassword' | 'newPassword' | null;

/**
 * "Segurança" screen (SPEC-0002 BR11/BR16, `Perfil → Segurança` — reached today via a direct
 * authenticated route, AC-8; the full Perfil menu is SPEC-0006). Authenticated password change
 * requires the correct current password (BR11); the new password mirrors the BR9 base policy plus
 * "differ from current" (this check applies to the change, not the recovery reset — DL-0003) and
 * a client-known match against the typed current password (a real hash comparison stays
 * server-side). Read-only login e-mail; a static mobile-biometrics info card (no backend, Out of
 * Scope).
 */
@Component({
  selector: 'app-security',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './security.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Security {
  private readonly api = inject(SecurityApi);
  private readonly auth = inject(AuthService);

  readonly email = computed(() => this.auth.username());
  readonly loading = signal(false);
  readonly success = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly errorField = signal<ErrorField>(null);
  readonly showCurrent = signal(false);
  readonly showNew = signal(false);

  currentPassword = '';
  newPassword = '';
  confirmPassword = '';

  get newPasswordValid(): boolean {
    return (
      meetsPasswordPolicy(this.newPassword) &&
      differsFromEmail(this.newPassword, this.email()) &&
      this.newPassword !== this.currentPassword
    );
  }

  get confirmValid(): boolean {
    return this.newPassword.length > 0 && this.newPassword === this.confirmPassword;
  }

  get formValid(): boolean {
    return this.currentPassword.length > 0 && this.newPasswordValid && this.confirmValid;
  }

  toggleCurrent(): void {
    this.showCurrent.update((visible) => !visible);
  }

  toggleNew(): void {
    this.showNew.update((visible) => !visible);
  }

  submit(): void {
    if (!this.formValid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.success.set(false);
    this.errorKey.set(null);
    this.errorField.set(null);
    this.api.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.success.set(true);
        this.loading.set(false);
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.applyError(error);
      },
    });
  }

  private applyError(error: HttpErrorResponse): void {
    switch (error.error?.code) {
      case 'auth.current-password-incorrect':
        this.errorKey.set('seguranca.erro.senhaAtualIncorreta');
        this.errorField.set('currentPassword');
        break;
      case 'auth.password-policy-violation':
        this.errorKey.set('seguranca.erro.senhaFraca');
        this.errorField.set('newPassword');
        break;
      default:
        this.errorKey.set('common.error');
        this.errorField.set(null);
    }
  }
}
