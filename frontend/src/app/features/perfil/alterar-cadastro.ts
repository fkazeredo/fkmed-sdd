import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  FIELD_MAX,
  isValidCep,
  isValidEmail,
  isValidLandline,
  isValidMobile,
  isValidUf,
} from './contact-validators';
import { BeneficiaryProfile, ContactUpdate, ProfileApi } from './profile.api';

/** The editable subset the form binds to (all strings) — BR5. */
type ContactFields = Pick<
  BeneficiaryProfile,
  | 'contactEmail'
  | 'mobile'
  | 'landline'
  | 'cep'
  | 'street'
  | 'number'
  | 'complement'
  | 'neighborhood'
  | 'city'
  | 'uf'
>;

const EDITABLE_KEYS: readonly (keyof ContactFields)[] = [
  'contactEmail',
  'mobile',
  'landline',
  'cep',
  'street',
  'number',
  'complement',
  'neighborhood',
  'city',
  'uf',
];

type ErrorField = 'contactEmail' | 'mobile' | 'landline' | 'cep' | 'uf' | null;

function emptyFields(): ContactFields {
  return {
    contactEmail: '',
    mobile: '',
    landline: '',
    cep: '',
    street: '',
    number: '',
    complement: '',
    neighborhood: '',
    city: '',
    uf: '',
  };
}

/**
 * "Alterar Cadastro" (SPEC-0006 BR4/BR5/BR6/BR7): the operator's contract data (name, CPF, birth
 * date, card number, plan) is shown read-only with the service-channel hint; contact and address
 * data are editable with per-field validation. Contact e-mail and mobile are mandatory and can
 * never be emptied (BR6). Saving is partial — only changed fields go in the PATCH (BR7) — and
 * targets the active beneficiary (SPEC-0003; the server re-validates scope, denying a dependent
 * who tries another's data, AC4).
 */
@Component({
  selector: 'app-alterar-cadastro',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './alterar-cadastro.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlterarCadastro {
  private readonly api = inject(ProfileApi);
  protected readonly context = inject(BeneficiaryContextService);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly saving = signal(false);
  readonly success = signal(false);
  readonly errorKey = signal<string | null>(null);
  readonly errorField = signal<ErrorField>(null);
  readonly profile = signal<BeneficiaryProfile | null>(null);

  form: ContactFields = emptyFields();
  private snapshot: ContactFields = emptyFields();
  readonly fieldMax = FIELD_MAX;

  constructor() {
    // Reload whenever the active beneficiary changes (SPEC-0003 BR5): initial load and every switch.
    effect(() => {
      const id = this.context.active()?.beneficiaryId;
      if (id) {
        this.load(id);
      }
    });
  }

  load(beneficiaryId: string): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.success.set(false);
    this.errorKey.set(null);
    this.errorField.set(null);
    this.api.getProfile(beneficiaryId).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        for (const key of EDITABLE_KEYS) {
          // Optional contact/address fields arrive as `null` when unset (ProfileView returns null
          // per field). The form and its validators operate on strings, so coerce null → '' at the
          // boundary — otherwise `form.landline.trim()` (and siblings) throw and break the form.
          // Optional contact/address fields arrive as `null` when unset (ProfileView returns null
          // per field). The form and its validators operate on strings, so coerce null → '' at the
          // boundary — otherwise `form.landline.trim()` (and siblings) throw and break the form.
          this.form[key] = profile[key] ?? '';
        }
        this.snapshot = { ...this.form };
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  get emailValid(): boolean {
    return isValidEmail(this.form.contactEmail);
  }

  get mobileValid(): boolean {
    return isValidMobile(this.form.mobile);
  }

  get landlineValid(): boolean {
    return this.form.landline.trim() === '' || isValidLandline(this.form.landline);
  }

  get cepValid(): boolean {
    return this.form.cep.trim() === '' || isValidCep(this.form.cep);
  }

  get ufValid(): boolean {
    return this.form.uf.trim() === '' || isValidUf(this.form.uf);
  }

  private get lengthsValid(): boolean {
    return (
      this.form.street.length <= FIELD_MAX.street &&
      this.form.number.length <= FIELD_MAX.number &&
      this.form.complement.length <= FIELD_MAX.complement &&
      this.form.neighborhood.length <= FIELD_MAX.neighborhood &&
      this.form.city.length <= FIELD_MAX.city
    );
  }

  get formValid(): boolean {
    return (
      this.emailValid &&
      this.mobileValid &&
      this.landlineValid &&
      this.cepValid &&
      this.ufValid &&
      this.lengthsValid
    );
  }

  /** Only the fields the user actually changed (BR7 partial update). */
  changedFields(): ContactUpdate {
    const changes: ContactUpdate = {};
    for (const key of EDITABLE_KEYS) {
      if (this.form[key] !== this.snapshot[key]) {
        changes[key] = this.form[key];
      }
    }
    return changes;
  }

  get hasChanges(): boolean {
    return Object.keys(this.changedFields()).length > 0;
  }

  submit(): void {
    const id = this.context.active()?.beneficiaryId;
    if (!id || !this.formValid || !this.hasChanges || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.success.set(false);
    this.errorKey.set(null);
    this.errorField.set(null);
    const changes = this.changedFields();
    this.api.updateContacts(id, changes).subscribe({
      next: () => {
        this.snapshot = { ...this.form };
        this.saving.set(false);
        this.success.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        this.applyError(error);
      },
    });
  }

  private applyError(error: HttpErrorResponse): void {
    const code = error.error?.code as string | undefined;
    const map: Record<string, ErrorField> = {
      'profile.mobile-required': 'mobile',
      'profile.mobile-invalid': 'mobile',
      'profile.contact-email-required': 'contactEmail',
      'profile.contact-email-invalid': 'contactEmail',
      'profile.landline-invalid': 'landline',
      'profile.cep-invalid': 'cep',
      'profile.uf-invalid': 'uf',
    };
    if (code && code in map) {
      this.errorKey.set(code);
      this.errorField.set(map[code]);
    } else {
      this.errorKey.set('common.error');
      this.errorField.set(null);
    }
  }
}
