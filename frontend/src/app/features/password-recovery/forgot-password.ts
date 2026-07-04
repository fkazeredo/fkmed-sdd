import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { RecoveryApi } from './recovery.api';

/**
 * "Esqueci minha senha" (SPEC-0002 BR10, public route): e-mail → recovery request. ALWAYS shows
 * the same neutral confirmation regardless of outcome (BR7/AC8) — the backend already answers
 * neutrally (202 for every e-mail); the UI must not turn a transport error into a "not found"
 * signal either, so both the success and error branches finish the same way.
 */
@Component({
  selector: 'app-forgot-password',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './forgot-password.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPassword {
  private readonly api = inject(RecoveryApi);
  private readonly auth = inject(AuthService);

  readonly loading = signal(false);
  readonly done = signal(false);
  email = '';

  get emailValid(): boolean {
    return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.email);
  }

  submit(): void {
    if (!this.emailValid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.api.request(this.email).subscribe({
      next: () => this.finish(),
      error: () => this.finish(),
    });
  }

  goToLogin(): void {
    this.auth.login();
  }

  private finish(): void {
    this.done.set(true);
    this.loading.set(false);
  }
}
