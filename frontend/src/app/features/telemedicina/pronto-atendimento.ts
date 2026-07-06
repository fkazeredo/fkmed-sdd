import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { SYMPTOM_DURATIONS } from './tele-time';
import { SymptomDuration, TeleApi, TeleCatalog } from './tele.api';

const COMPLAINT_MIN = 10;
const COMPLAINT_MAX = 500;
type Step = 'triagem' | 'termo';

/**
 * Pronto Atendimento — triage (BR2), emergency-signal alert (BR3), teleattendance term (BR4) and
 * queue entry (BR5/BR7). A two-step flow (triagem → termo) in one component: the triage gate blocks
 * advancing without a valid complaint (10–500 chars, live count) and a chosen duration; selecting an
 * emergency symptom raises the ER alert that must be acknowledged ("proceder mesmo assim") before
 * advancing. The term must be accepted before `POST /api/tele/sessions`; a `422 tele.complaint-invalid`
 * returns to triage and `422 tele.term-not-accepted` shows inline on the term. On success (a new
 * `201` OR a resumed `200`, BR7) it routes to the live session screen. If a session is already active
 * on entry, it routes straight there without re-triaging (AC5).
 */
@Component({
  selector: 'app-pronto-atendimento',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './pronto-atendimento.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProntoAtendimento implements OnInit {
  private readonly api = inject(TeleApi);
  private readonly router = inject(Router);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly step = signal<Step>('triagem');
  protected readonly loading = signal(true);
  protected readonly catalog = signal<TeleCatalog | null>(null);
  protected readonly submitting = signal(false);
  protected readonly gateKey = signal<string | null>(null);
  protected readonly errorKey = signal<string | null>(null);

  // Triage inputs (BR2).
  protected readonly complaint = signal('');
  protected readonly selectedSymptoms = signal<string[]>([]);
  protected readonly otherSymptom = signal('');
  protected readonly duration = signal<SymptomDuration | null>(null);
  protected readonly emergencyAcknowledged = signal(false);

  // Term (BR4).
  protected readonly termAccepted = signal(false);

  protected readonly durations = SYMPTOM_DURATIONS;

  protected readonly beneficiaryName = computed(() => this.context.active()?.firstName ?? '');
  protected readonly symptoms = computed(() => this.catalog()?.symptoms ?? []);
  protected readonly term = computed(() => this.catalog()?.term ?? null);

  protected readonly complaintLength = computed(() => this.complaint().trim().length);
  protected readonly complaintValid = computed(() => {
    const length = this.complaintLength();
    return length >= COMPLAINT_MIN && length <= COMPLAINT_MAX;
  });

  /** BR3: an emergency signal is any selected symptom the catalog flags `emergency`. */
  protected readonly hasEmergencySelected = computed(() =>
    this.symptoms().some((symptom) => symptom.emergency && this.selectedSymptoms().includes(symptom.code)),
  );
  /** The ER alert blocks advancing until the user chooses to proceed anyway (BR3). */
  protected readonly emergencyBlocking = computed(
    () => this.hasEmergencySelected() && !this.emergencyAcknowledged(),
  );

  ngOnInit(): void {
    // AC5: if a session is already active, skip triage and go straight to it.
    this.api.getCurrentSession().subscribe({
      next: (session) => {
        if (session.state === 'EM_FILA' || session.state === 'EM_ATENDIMENTO') {
          void this.router.navigate(['/telemedicina/sessao']);
        } else {
          this.loadCatalog();
        }
      },
      error: () => this.loadCatalog(),
    });
  }

  isSymptomSelected(code: string): boolean {
    return this.selectedSymptoms().includes(code);
  }

  toggleSymptom(code: string): void {
    const current = this.selectedSymptoms();
    const next = current.includes(code)
      ? current.filter((value) => value !== code)
      : [...current, code];
    this.selectedSymptoms.set(next);
    // Deselecting the emergency symptom clears an earlier acknowledgment (the alert may reappear).
    if (!this.hasEmergencySelected()) {
      this.emergencyAcknowledged.set(false);
    }
    this.gateKey.set(null);
  }

  selectDuration(value: SymptomDuration): void {
    this.duration.set(value);
    this.gateKey.set(null);
  }

  acknowledgeEmergency(): void {
    this.emergencyAcknowledged.set(true);
  }

  goToER(): void {
    void this.router.navigate(['/rede/busca']);
  }

  goToTerm(): void {
    if (!this.complaintValid()) {
      this.gateKey.set('telemedicina.gate.queixa');
      return;
    }
    if (!this.duration()) {
      this.gateKey.set('telemedicina.gate.duracao');
      return;
    }
    if (this.emergencyBlocking()) {
      this.gateKey.set('telemedicina.gate.emergencia');
      return;
    }
    this.gateKey.set(null);
    this.step.set('termo');
  }

  backToTriage(): void {
    this.step.set('triagem');
    this.errorKey.set(null);
  }

  enterQueue(): void {
    const beneficiaryId = this.context.active()?.beneficiaryId;
    const currentTerm = this.term();
    const duration = this.duration();
    if (!beneficiaryId || !currentTerm || !duration || this.submitting()) {
      return;
    }
    if (!this.termAccepted()) {
      this.errorKey.set('tele.term-not-accepted');
      return;
    }
    this.submitting.set(true);
    this.errorKey.set(null);
    const other = this.otherSymptom().trim();
    this.api
      .createSession({
        beneficiaryId,
        complaint: this.complaint().trim(),
        symptoms: this.selectedSymptoms(),
        otherSymptom: other || undefined,
        duration,
        termVersion: currentTerm.version,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/telemedicina/sessao']);
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          this.applyError(error);
        },
      });
  }

  private applyError(error: HttpErrorResponse): void {
    const code = error.error?.code;
    if (code === 'tele.complaint-invalid') {
      this.step.set('triagem');
      this.errorKey.set(null);
      this.gateKey.set('telemedicina.gate.queixa');
      return;
    }
    this.errorKey.set(code === 'tele.term-not-accepted' ? code : 'common.error');
  }

  private loadCatalog(): void {
    this.loading.set(true);
    this.api.getCatalog().subscribe({
      next: (catalog) => {
        this.catalog.set(catalog);
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }
}
