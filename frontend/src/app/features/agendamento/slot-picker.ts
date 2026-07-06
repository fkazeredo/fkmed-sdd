import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AvailabilityDay, AvailabilitySlot } from './appointments.api';

const WEEKDAYS_PT = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

/** TZ-safe day label from a `YYYY-MM-DD` string (no `Date('...')` parsing that shifts by timezone).
 * e.g. `"2026-07-10"` → `"sex 10/07"`. */
export function formatDayLabel(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const weekday = WEEKDAYS_PT[new Date(year, month - 1, day).getDay()];
  return `${weekday} ${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}`;
}

/** `"2026-07-10T09:00"` → `"09:00"` (the HH:mm straight from the ISO local string). */
export function formatSlotTime(isoSlot: string): string {
  return isoSlot.split('T')[1]?.slice(0, 5) ?? isoSlot;
}

/** BR5/BR6: a slot is selectable only while flagged available AND with remaining capacity. */
export function isSlotOpen(slot: AvailabilitySlot): boolean {
  return slot.available && slot.remaining > 0;
}

/**
 * Presentational calendar/time step (SPEC-0009 BR5/BR6) shared by both wizards and the reschedule
 * dialog: the backend already returns only today→+30 d days ≥ 2 h ahead (DL-0013), so this renders
 * exactly the days it receives — no past/no-agenda day appears. Full slots render unselectable
 * (BR5). Picks a day, then a time; emits the chosen slot's ISO datetime.
 */
@Component({
  selector: 'app-slot-picker',
  imports: [TranslatePipe],
  templateUrl: './slot-picker.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlotPicker {
  readonly days = input.required<AvailabilityDay[]>();
  readonly selectedSlot = input<string | null>(null);
  readonly slotSelected = output<string>();

  readonly selectedDate = signal<string | null>(null);

  readonly currentDay = computed<AvailabilityDay | null>(
    () => this.days().find((day) => day.date === this.selectedDate()) ?? null,
  );

  constructor() {
    // On (re)entry with a pre-chosen slot (back navigation / reschedule), open that slot's day.
    effect(() => {
      const slot = this.selectedSlot();
      if (slot && !this.selectedDate()) {
        this.selectedDate.set(slot.split('T')[0]);
      }
    });
  }

  dayLabel(isoDate: string): string {
    return formatDayLabel(isoDate);
  }

  slotTime(isoSlot: string): string {
    return formatSlotTime(isoSlot);
  }

  open(slot: AvailabilitySlot): boolean {
    return isSlotOpen(slot);
  }

  pickDay(date: string): void {
    this.selectedDate.set(date);
  }

  pickSlot(slot: AvailabilitySlot): void {
    if (this.open(slot)) {
      this.slotSelected.emit(slot.slot);
    }
  }
}
