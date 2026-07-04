import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { isValidCpf } from './cpf';
import {
  CompleteFirstAccessRequest,
  FirstAccessApi,
  VerifyFirstAccessResponse,
} from './first-access.api';

/**
 * "Primeiro acesso" wizard (SPEC-0002): step 1 verifies the identity triple, step 2 creates the
 * account (e-mail + password with show/hide + Terms/Privacy acceptance), step 3 asks the user to
 * check their e-mail. Runs against the real contract (docs/api/openapi.json); all messages come from
 * the pt-BR bundle; errors stay inline and neutral (BR7/BR16).
 */
@Component({
  selector: 'app-first-access',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './first-access.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FirstAccess {
  private readonly api = inject(FirstAccessApi);
  private readonly auth = inject(AuthService);

  readonly step = signal(1);
  readonly loading = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly accountExists = computed(() => this.errorKey() === 'primeiroAcesso.erro.jaExiste');

  cpf = '';
  cardNumber = '';
  birthDate = '';
  email = '';
  password = '';
  acceptedTerms = false;
  acceptedPrivacy = false;
  private registrationToken = '';

  get cpfValid(): boolean {
    return isValidCpf(this.cpf);
  }

  get cardValid(): boolean {
    return /^\d{9}$/.test(this.cardNumber);
  }

  get birthValid(): boolean {
    return this.birthDate.length > 0 && new Date(this.birthDate).getTime() < Date.now();
  }

  get emailValid(): boolean {
    return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(this.email);
  }

  /** Client mirror of the server password policy (BR16); the server remains authoritative (BR9). */
  get passwordValid(): boolean {
    return (
      this.password.length >= 8 &&
      /[a-zA-Z]/.test(this.password) &&
      /\d/.test(this.password) &&
      this.password.trim().toLowerCase() !== this.email.trim().toLowerCase()
    );
  }

  get step1Valid(): boolean {
    return this.cpfValid && this.cardValid && this.birthValid;
  }

  get step2Valid(): boolean {
    return this.emailValid && this.passwordValid && this.acceptedTerms && this.acceptedPrivacy;
  }

  submitStep1(): void {
    if (!this.step1Valid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.verify({ cpf: this.cpf, cardNumber: this.cardNumber, birthDate: this.birthDate }).subscribe({
      next: (response: VerifyFirstAccessResponse) => {
        this.registrationToken = response.registrationToken;
        this.step.set(2);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorKey.set(this.messageFor(error));
        this.loading.set(false);
      },
    });
  }

  submitStep2(): void {
    if (!this.step2Valid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    const request: CompleteFirstAccessRequest = {
      registrationToken: this.registrationToken,
      email: this.email,
      password: this.password,
      acceptedTerms: this.acceptedTerms,
      acceptedPrivacy: this.acceptedPrivacy,
    };
    this.api.complete(request).subscribe({
      next: () => {
        this.step.set(3);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorKey.set(this.messageFor(error));
        this.loading.set(false);
      },
    });
  }

  goToLogin(): void {
    this.auth.login();
  }

  togglePassword(): void {
    this.showPassword.update((visible) => !visible);
  }

  private messageFor(error: HttpErrorResponse): string {
    switch (error.error?.code) {
      case 'auth.registration-not-found':
        return 'primeiroAcesso.erro.naoEncontrado';
      case 'auth.dependent-underage':
        return 'primeiroAcesso.erro.menorIdade';
      case 'auth.account-already-exists':
        return 'primeiroAcesso.erro.jaExiste';
      case 'auth.email-already-used':
        return 'primeiroAcesso.erro.emailEmUso';
      case 'auth.password-policy-violation':
        return 'primeiroAcesso.erro.senhaFraca';
      default:
        return 'common.error';
    }
  }
}
