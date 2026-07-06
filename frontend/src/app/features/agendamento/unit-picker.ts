import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CareUnit } from './appointments.api';

/**
 * Presentational unit step shared by both wizards (SPEC-0009 BR3/BR4): lists the own units serving
 * the chosen specialty/exam and emits the selected unit id. Selection is highlighted; the parent
 * wizard owns the "Próximo" gating (BR2). Empty list renders a real empty state.
 */
@Component({
  selector: 'app-unit-picker',
  imports: [TranslatePipe],
  templateUrl: './unit-picker.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnitPicker {
  readonly units = input.required<CareUnit[]>();
  readonly selectedId = input<string | null>(null);
  readonly unitSelected = output<string>();

  select(id: string): void {
    this.unitSelected.emit(id);
  }
}
