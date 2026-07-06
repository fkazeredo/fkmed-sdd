import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { CareUnit } from './appointments.api';
import { UnitPicker } from './unit-picker';

const UNITS: CareUnit[] = [
  { id: 'u1', name: 'Unidade Centro', address: 'Rua A, 10 — Centro, Rio de Janeiro – RJ' },
  { id: 'u2', name: 'Unidade Tijuca', address: 'Rua B, 20 — Tijuca, Rio de Janeiro – RJ' },
];

describe('UnitPicker (BR3/BR4)', () => {
  let fixture: ComponentFixture<UnitPicker>;
  let ref: ComponentRef<UnitPicker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UnitPicker],
      providers: [provideI18n()],
    }).compileComponents();
    fixture = TestBed.createComponent(UnitPicker);
    ref = fixture.componentRef;
    ref.setInput('units', UNITS);
    fixture.detectChanges();
  });

  it('renders each unit with its name and address', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="unit-option-u1"]')?.textContent).toContain('Unidade Centro');
    expect(el.querySelector('[data-testid="unit-option-u1-endereco"]')?.textContent).toContain(
      'Rua A, 10 — Centro',
    );
    expect(el.querySelector('[data-testid="unit-option-u2"]')?.textContent).toContain('Unidade Tijuca');
  });

  it('emits the unit id when a unit is clicked', () => {
    const selected: string[] = [];
    fixture.componentInstance.unitSelected.subscribe((id) => selected.push(id));
    (fixture.nativeElement.querySelector('[data-testid="unit-option-u2"]') as HTMLElement).click();
    expect(selected).toEqual(['u2']);
  });

  it('highlights the selected unit', () => {
    ref.setInput('selectedId', 'u1');
    fixture.detectChanges();
    const option = fixture.nativeElement.querySelector('[data-testid="unit-option-u1"]') as HTMLElement;
    expect(option.getAttribute('aria-pressed')).toBe('true');
  });

  it('renders the empty state when no unit serves the scope', () => {
    ref.setInput('units', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="unit-picker-vazio"]')).not.toBeNull();
  });
});
