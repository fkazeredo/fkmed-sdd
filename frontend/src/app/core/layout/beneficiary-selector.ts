import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SelectModule } from 'primeng/select';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';

/**
 * Active-beneficiary selector shown in the shell header (SPEC-0003 BR5): avatar placeholder,
 * first name and role of the active beneficiary ("MARIA · Responsável"); lists exactly the
 * beneficiaries accessible to the user. A single accessible beneficiary — a dependent's own
 * session, which BR1 limits to themselves — renders as a static label: there is nothing to
 * switch to. Switching only updates the client-side context (BeneficiaryContextService); the
 * server re-validates the target beneficiary of every request regardless (BR3).
 */
@Component({
  selector: 'app-beneficiary-selector',
  imports: [FormsModule, SelectModule, TranslatePipe],
  templateUrl: './beneficiary-selector.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BeneficiarySelector {
  protected readonly context = inject(BeneficiaryContextService);
  private readonly translate = inject(TranslateService);

  readonly hasChoice = computed(() => this.context.accessible().length > 1);

  roleLabel(role: string | undefined): string {
    return role ? this.translate.instant(`contexto.papel.${role}`) : '';
  }

  initialOf(firstName: string | undefined): string {
    return firstName ? firstName.charAt(0).toUpperCase() : '?';
  }

  onChange(event: { value: string }): void {
    this.context.setActive(event.value);
  }
}
