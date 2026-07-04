import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { meetsPasswordPolicy } from '../../shared/validators/password-policy';
import { RecoveryApi } from './recovery.api';

type ResetStatus = 'form' | 'success' | 'invalid';

/**
 * "Redefinir senha" (SPEC-0002 BR10, public route reached from the e-mailed link): new password +
 * confirmation with show/hide and the BR16 client-side policy mirror (BR9 base rule only — DL-0003
 * scopes "must differ from current" to the authenticated change, not the reset, since the user
 * proved e-mail ownership and does not know/remember the old password). A missing/invalid/used/
 * expired link (410, AC5) renders a distinct "link inválido" state rather than a generic error. A
 * successful reset terminates all sessions server-side (BR10); the SPA just directs to a fresh
 * login.
 */
@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './reset-password.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPassword implements OnInit {
  private readonly api = inject(RecoveryApi);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly status = signal<ResetStatus>('form');
  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly errorKey = signal<string | null>(null);
  private token = '';

  newPassword = '';
  confirmPassword = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status.set('invalid');
      return;
    }
    this.token = token;
  }

  get passwordValid(): boolean {
    return meetsPasswordPolicy(this.newPassword);
  }

  get confirmValid(): boolean {
    return this.newPassword.length > 0 && this.newPassword === this.confirmPassword;
  }

  get formValid(): boolean {
    return this.passwordValid && this.confirmValid;
  }

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  submit(): void {
    if (!this.formValid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.reset(this.token, this.newPassword).subscribe({
      next: () => {
        this.status.set('success');
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.status === 410) {
          this.status.set('invalid');
          return;
        }
        this.errorKey.set(
          error.error?.code === 'auth.password-policy-violation'
            ? 'redefinirSenha.erro.senhaFraca'
            : 'common.error',
        );
      },
    });
  }

  goToLogin(): void {
    this.auth.login();
  }
}
