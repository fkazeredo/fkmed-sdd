import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { EmailVerificationApi } from './email-verification.api';

type VerificationStatus = 'loading' | 'confirmed' | 'invalid' | 'idle';

/**
 * "Verificação de e-mail" landing (SPEC-0002 BR5): confirms the account from the link token; an
 * expired/invalid link offers a neutral resend (BR7). Reached from the verification e-mail and from
 * the login page's unverified affordance (BR6).
 */
@Component({
  selector: 'app-email-verification',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './email-verification.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailVerification implements OnInit {
  private readonly api = inject(EmailVerificationApi);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly status = signal<VerificationStatus>('loading');
  readonly resending = signal(false);
  readonly resendDone = signal(false);
  resendEmail = '';

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status.set('idle');
      return;
    }
    this.api.confirm(token).subscribe({
      next: () => this.status.set('confirmed'),
      error: () => this.status.set('invalid'),
    });
  }

  get emailValid(): boolean {
    return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.resendEmail);
  }

  resend(): void {
    if (!this.emailValid || this.resending()) {
      return;
    }
    this.resending.set(true);
    // Neutral by design (BR7): the same "we sent it if applicable" outcome regardless of result.
    this.api.resend(this.resendEmail).subscribe({
      next: () => this.finishResend(),
      error: () => this.finishResend(),
    });
  }

  goToLogin(): void {
    this.auth.login();
  }

  private finishResend(): void {
    this.resendDone.set(true);
    this.resending.set(false);
  }
}
