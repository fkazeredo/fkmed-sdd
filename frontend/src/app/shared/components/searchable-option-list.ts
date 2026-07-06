import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

export interface SelectableOption {
  value: string;
  label: string;
}

interface OptionGroup {
  letter: string;
  options: SelectableOption[];
}

const COMBINING_DIACRITICS = new RegExp('[̀-ͯ]', 'g');

/** Strips diacritics and lowercases, so "niteroi"/"NITERÓI"/"Niterói" all match (BR2/BR6). */
function normalize(value: string): string {
  return value.normalize('NFD').replace(COMBINING_DIACRITICS, '').toLowerCase();
}

/**
 * Reusable selection-screen widget (SPEC-0008 BR2 — State/Municipality/Neighborhood pickers —
 * and BR6 — the Specialty picker): a search box that filters in real time (accent/case
 * insensitive), an alphabetical grouping of the results, and the "Nenhum resultado para
 * '{termo}'" empty state. Filtering happens locally against whatever `items` the parent passes in
 * — for callers backed by a client-side-complete list (State, Neighborhood, Specialty) that is the
 * whole story; for a server-driven list (Municipality, whose `/municipalities?q=` narrows on the
 * backend as the query grows) the parent also listens to `queryChange` and refetches, replacing
 * `items` — local filtering keeps working unchanged on top of whatever list arrives.
 */
@Component({
  selector: 'app-searchable-option-list',
  imports: [TranslatePipe],
  templateUrl: './searchable-option-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchableOptionList {
  readonly items = input.required<SelectableOption[]>();

  readonly selected = output<string>();
  readonly queryChange = output<string>();

  readonly query = signal('');

  readonly filtered = computed(() => {
    const normalizedQuery = normalize(this.query().trim());
    if (!normalizedQuery) {
      return this.items();
    }
    return this.items().filter((item) => normalize(item.label).includes(normalizedQuery));
  });

  readonly groups = computed<OptionGroup[]>(() => {
    const sorted = [...this.filtered()].sort((a, b) => a.label.localeCompare(b.label, 'pt-BR'));
    const byLetter = new Map<string, SelectableOption[]>();
    for (const option of sorted) {
      const letter = option.label.charAt(0).toUpperCase();
      const bucket = byLetter.get(letter);
      if (bucket) {
        bucket.push(option);
      } else {
        byLetter.set(letter, [option]);
      }
    }
    return Array.from(byLetter.entries()).map(([letter, options]) => ({ letter, options }));
  });

  onQueryInput(value: string): void {
    this.query.set(value);
    this.queryChange.emit(value);
  }

  select(value: string): void {
    this.selected.emit(value);
  }
}
