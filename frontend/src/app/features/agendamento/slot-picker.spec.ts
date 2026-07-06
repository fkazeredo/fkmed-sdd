import { ComponentRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AvailabilityDay } from './appointments.api';
import { formatDayLabel, formatSlotTime, isSlotOpen, SlotPicker } from './slot-picker';

const DAYS: AvailabilityDay[] = [
  {
    date: '2026-07-10',
    slots: [
      { slot: '2026-07-10T09:00', remaining: 2, available: true },
      { slot: '2026-07-10T09:30', remaining: 0, available: false },
    ],
  },
  {
    date: '2026-07-11',
    slots: [{ slot: '2026-07-11T10:00', remaining: 1, available: true }],
  },
];

describe('slot-picker pure helpers', () => {
  it('formatSlotTime extracts HH:mm from the ISO local slot', () => {
    expect(formatSlotTime('2026-07-10T09:00')).toBe('09:00');
    expect(formatSlotTime('2026-07-11T14:30')).toBe('14:30');
  });

  it('formatDayLabel is TZ-safe and shows the weekday + DD/MM', () => {
    expect(formatDayLabel('2026-07-10')).toContain('10/07');
    // 2026-07-10 is a Friday.
    expect(formatDayLabel('2026-07-10')).toContain('sex');
  });

  it('isSlotOpen requires available AND remaining capacity (BR5/BR6)', () => {
    expect(isSlotOpen({ slot: 's', remaining: 2, available: true })).toBe(true);
    expect(isSlotOpen({ slot: 's', remaining: 0, available: true })).toBe(false);
    expect(isSlotOpen({ slot: 's', remaining: 3, available: false })).toBe(false);
  });
});

describe('SlotPicker (BR5/BR6)', () => {
  let fixture: ComponentFixture<SlotPicker>;
  let ref: ComponentRef<SlotPicker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SlotPicker],
      providers: [provideI18n()],
    }).compileComponents();
    fixture = TestBed.createComponent(SlotPicker);
    ref = fixture.componentRef;
    ref.setInput('days', DAYS);
    fixture.detectChanges();
  });

  it('renders one button per available day and no time grid until a day is picked', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="slot-day-2026-07-10"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="slot-day-2026-07-11"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="slot-time-2026-07-10T09:00"]')).toBeNull();
  });

  it('shows the day\'s slots after picking a day, with full slots disabled (BR5)', () => {
    (fixture.nativeElement.querySelector('[data-testid="slot-day-2026-07-10"]') as HTMLElement).click();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const open = el.querySelector('[data-testid="slot-time-2026-07-10T09:00"]') as HTMLButtonElement;
    const full = el.querySelector('[data-testid="slot-time-2026-07-10T09:30"]') as HTMLButtonElement;
    expect(open.disabled).toBe(false);
    expect(full.disabled).toBe(true);
  });

  it('emits the chosen slot when an open time is clicked', () => {
    const selected: string[] = [];
    fixture.componentInstance.slotSelected.subscribe((s) => selected.push(s));
    (fixture.nativeElement.querySelector('[data-testid="slot-day-2026-07-10"]') as HTMLElement).click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('[data-testid="slot-time-2026-07-10T09:00"]') as HTMLElement).click();
    expect(selected).toEqual(['2026-07-10T09:00']);
  });

  it('does not emit when a full slot is clicked (BR5)', () => {
    const selected: string[] = [];
    fixture.componentInstance.slotSelected.subscribe((s) => selected.push(s));
    (fixture.nativeElement.querySelector('[data-testid="slot-day-2026-07-10"]') as HTMLElement).click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('[data-testid="slot-time-2026-07-10T09:30"]') as HTMLElement).click();
    expect(selected).toEqual([]);
  });

  it('pre-opens the day of a pre-selected slot (back navigation / reschedule)', () => {
    ref.setInput('selectedSlot', '2026-07-11T10:00');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="slot-time-2026-07-11T10:00"]')).not.toBeNull();
  });

  it('renders the empty state when the unit has no availability', () => {
    ref.setInput('days', []);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="slot-picker-vazio"]')).not.toBeNull();
  });
});
