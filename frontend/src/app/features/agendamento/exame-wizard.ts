import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SelectableOption, SearchableOptionList } from '../../shared/components/searchable-option-list';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { checkAttachment } from './appointment-attachment';
import { AppointmentsApi, AvailabilityDay, CareUnit, RegistryOption } from './appointments.api';
import { formatDayLabel, formatSlotTime } from './slot-picker';
import { SlotPicker } from './slot-picker';
import { UnitPicker } from './unit-picker';

/** The six exam-wizard steps (BR4). */
type Step = 'exam' | 'attachment' | 'unit' | 'slot' | 'review' | 'success';
const FLOW: Step[] = ['exam', 'attachment', 'unit', 'slot', 'review', 'success'];

/**
 * Exam booking wizard (SPEC-0009 BR4): exam → mandatory medical-order upload → unit → date/time →
 * review → confirm. The attachment step enforces the client pre-check (JPG/PNG/PDF ≤ 5 MB by real
 * content, DL-0015) with a name preview and removal, and BR2 blocks advancing without a valid file.
 * Confirmation posts multipart (BR7); slot-taken returns to the time step (BR6). Binds to the ACTIVE
 * beneficiary at confirmation (BR1).
 */
@Component({
  selector: 'app-exame-wizard',
  imports: [TranslatePipe, SearchableOptionList, UnitPicker, SlotPicker],
  templateUrl: './exame-wizard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExameWizard implements OnInit {
  private readonly api = inject(AppointmentsApi);
  private readonly router = inject(Router);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly step = signal<Step>('exam');
  protected readonly loading = signal(false);
  protected readonly gateKey = signal<string | null>(null);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly protocol = signal<string | null>(null);

  protected readonly exams = signal<RegistryOption[]>([]);
  protected readonly units = signal<CareUnit[]>([]);
  protected readonly days = signal<AvailabilityDay[]>([]);

  protected readonly examCode = signal<string | null>(null);
  protected readonly examName = signal<string | null>(null);
  protected readonly attachment = signal<File | null>(null);
  protected readonly attachmentName = signal<string | null>(null);
  protected readonly attachmentError = signal<string | null>(null);
  protected readonly unitId = signal<string | null>(null);
  protected readonly slot = signal<string | null>(null);

  protected readonly examOptions = computed<SelectableOption[]>(() =>
    this.exams().map((option) => ({ value: option.code, label: option.name })),
  );
  protected readonly unitName = computed(
    () => this.units().find((unit) => unit.id === this.unitId())?.name ?? '',
  );
  protected readonly beneficiaryName = computed(() => this.context.active()?.firstName ?? '');
  protected readonly slotLabel = computed(() => {
    const value = this.slot();
    return value ? `${formatDayLabel(value.split('T')[0])} · ${formatSlotTime(value)}` : '';
  });

  ngOnInit(): void {
    this.api.getExams().subscribe((list) => this.exams.set(list));
  }

  selectExam(code: string): void {
    const found = this.exams().find((option) => option.code === code);
    if (!found) {
      return;
    }
    this.examCode.set(found.code);
    this.examName.set(found.name);
    this.unitId.set(null);
    this.slot.set(null);
    this.gateKey.set(null);
  }

  /** BR4 client pre-check: sniff the real content + size, keep or refuse with a clear message. */
  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (file) {
      await this.acceptFile(file);
    }
  }

  async acceptFile(file: File): Promise<void> {
    this.attachmentError.set(null);
    this.gateKey.set(null);
    const header = new Uint8Array(await file.arrayBuffer());
    const rejection = checkAttachment(file.size, header.subarray(0, 8));
    if (rejection) {
      this.attachment.set(null);
      this.attachmentName.set(null);
      this.attachmentError.set(rejection);
      return;
    }
    this.attachment.set(file);
    this.attachmentName.set(file.name);
  }

  removeAttachment(): void {
    this.attachment.set(null);
    this.attachmentName.set(null);
    this.attachmentError.set(null);
  }

  selectUnit(id: string): void {
    this.unitId.set(id);
    this.slot.set(null);
    this.gateKey.set(null);
  }

  selectSlot(value: string): void {
    this.slot.set(value);
    this.gateKey.set(null);
    this.errorKey.set(null);
  }

  next(): void {
    const current = this.step();
    if (current === 'exam') {
      if (!this.examCode()) {
        this.gateKey.set('agendamento.gate.exame');
        return;
      }
      this.go('attachment');
    } else if (current === 'attachment') {
      if (!this.attachment()) {
        this.gateKey.set('agendamento.gate.anexo');
        return;
      }
      this.loadUnits();
      this.go('unit');
    } else if (current === 'unit') {
      if (!this.unitId()) {
        this.gateKey.set('agendamento.gate.unidade');
        return;
      }
      this.loadAvailability();
      this.go('slot');
    } else if (current === 'slot') {
      if (!this.slot()) {
        this.gateKey.set('agendamento.gate.horario');
        return;
      }
      this.go('review');
    }
  }

  back(): void {
    const index = FLOW.indexOf(this.step());
    if (index > 0) {
      this.go(FLOW[index - 1]);
    }
  }

  confirm(): void {
    const beneficiaryId = this.context.active()?.beneficiaryId;
    const exam = this.examCode();
    const unitId = this.unitId();
    const slot = this.slot();
    const file = this.attachment();
    if (!beneficiaryId || !exam || !unitId || !slot || !file || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.bookExam({ beneficiaryId, exam, unitId, slot, file }).subscribe({
      next: (confirmation) => {
        this.loading.set(false);
        this.protocol.set(confirmation.protocol);
        this.go('success');
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.applyBookingError(error);
      },
    });
  }

  goToMeus(): void {
    void this.router.navigate(['/agendamento/meus']);
  }

  private go(step: Step): void {
    this.gateKey.set(null);
    this.step.set(step);
  }

  private loadUnits(): void {
    this.api.getUnits({ exam: this.examCode() ?? undefined }).subscribe((list) => this.units.set(list));
  }

  private loadAvailability(): void {
    const unitId = this.unitId();
    if (!unitId) {
      return;
    }
    this.api
      .getAvailability({ unitId, exam: this.examCode() ?? undefined })
      .subscribe((days) => this.days.set(days));
  }

  private applyBookingError(error: HttpErrorResponse): void {
    const code = error.error?.code;
    if (code === 'appointment.slot-taken') {
      this.slot.set(null);
      this.loadAvailability();
      this.go('slot');
      this.errorKey.set('appointment.slot-taken');
      return;
    }
    if (code === 'appointment.attachment-required' || code === 'appointment.attachment-invalid') {
      // Defensive: server refused the file — send the user back to the attachment step.
      this.attachment.set(null);
      this.attachmentName.set(null);
      this.go('attachment');
      this.attachmentError.set(code);
      return;
    }
    this.errorKey.set(
      code === 'appointment.time-conflict' || code === 'appointment.outside-horizon'
        ? code
        : 'common.error',
    );
  }
}
