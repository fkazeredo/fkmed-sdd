import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { SearchableOptionList, SelectableOption } from './searchable-option-list';

const OPTIONS: SelectableOption[] = [
  { value: 'rio-de-janeiro', label: 'Rio de Janeiro' },
  { value: 'rio-bonito', label: 'Rio Bonito' },
  { value: 'niteroi', label: 'Niterói' },
  { value: 'cabo-frio', label: 'Cabo Frio' },
];

@Component({
  selector: 'app-host',
  imports: [SearchableOptionList],
  template: `
    <app-searchable-option-list
      [items]="items"
      (selected)="onSelected($event)"
      (queryChange)="onQueryChange($event)"
    />
  `,
})
class HostComponent {
  items = OPTIONS;
  selectedValue: string | null = null;
  lastQuery: string | null = null;

  onSelected(value: string): void {
    this.selectedValue = value;
  }

  onQueryChange(query: string): void {
    this.lastQuery = query;
  }
}

/**
 * SPEC-0008 BR2/BR6: reusable searchable, alphabetically grouped selection list shared by the
 * State/Municipality/Neighborhood pickers (BR2) and the Specialty picker (BR6). Filters in real
 * time, accent- and case-insensitive; empty state "Nenhum resultado para '{termo}'".
 */
describe('SearchableOptionList', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideI18n()],
    }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  function typeQuery(value: string): void {
    const input = fixture.nativeElement.querySelector('[data-testid="option-search-input"]') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('renders every option grouped by initial letter, alphabetically', () => {
    const groupLetters = Array.from(
      fixture.nativeElement.querySelectorAll('[data-testid^="option-group-"]'),
    ).map((el) => (el as HTMLElement).getAttribute('data-testid'));
    expect(groupLetters).toEqual(['option-group-C', 'option-group-N', 'option-group-R']);
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]')).toHaveLength(4);
  });

  it('filters in real time, case- and accent-insensitive (BR2, AC1)', () => {
    typeQuery('rio de');
    const items = Array.from(fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]')) as HTMLElement[];
    expect(items).toHaveLength(1);
    expect(items.map((el) => el.textContent?.trim())).toEqual(['Rio de Janeiro']);
  });

  it('matches accents typed without diacritics (BR2)', () => {
    typeQuery('niteroi');
    const items = fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]');
    expect(items).toHaveLength(1);
    expect(items[0].textContent).toContain('Niterói');
  });

  it('matches uppercase queries (BR2)', () => {
    typeQuery('CABO');
    const items = fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]');
    expect(items).toHaveLength(1);
    expect(items[0].textContent).toContain('Cabo Frio');
  });

  it('shows the empty state with the typed term when nothing matches (BR2)', () => {
    typeQuery('xyz');
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]')).toHaveLength(0);
    const empty = fixture.nativeElement.querySelector('[data-testid="option-list-empty"]');
    expect(empty?.textContent).toContain("Nenhum resultado para 'xyz'");
  });

  it('emits selected() with the value when an item is clicked', () => {
    (fixture.nativeElement.querySelector('[data-testid="option-item-niteroi"]') as HTMLElement).click();
    expect(fixture.componentInstance.selectedValue).toBe('niteroi');
  });

  it('emits queryChange() on every keystroke (server-side search hook, e.g. municipalities)', () => {
    typeQuery('rio');
    expect(fixture.componentInstance.lastQuery).toBe('rio');
  });
});
