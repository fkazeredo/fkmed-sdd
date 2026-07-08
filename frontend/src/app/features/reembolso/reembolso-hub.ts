import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, WritableSignal, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  CatalogView,
  DocumentCategory,
  ReimbursementApi,
  ReimbursementDetailView,
  ReimbursementHistoryItem,
  ReimbursementPreviewListItem,
  ReimbursementPreviewResult,
  ReimbursementStatementView,
  ReimbursementSubmissionResult,
  SubmitReimbursementPayload,
  UploadedFileEntry,
} from './reimbursement.api';
import { formatBrDate, formatBrl, statusBadge } from './reimbursement-format';
import { totalBytes, validateReimbursementFile } from './reimbursement-upload';

type ReembolsoTab = 'solicitar' | 'historico' | 'extrato' | 'previas';

interface RequestForm extends SubmitReimbursementPayload {
  acceptedTerm: boolean;
}

const EMPTY_FORM: RequestForm = {
  beneficiaryId: '',
  expenseTypeCode: 'CONSULTA',
  careDate: '',
  amount: 0,
  sessions: [],
  providerName: '',
  providerCouncilCode: 'CRM',
  providerCouncilNumber: '',
  providerCouncilUf: 'RJ',
  providerDocument: '',
  providerSpecialty: '',
  bankCode: '001',
  bankAgency: '',
  bankAccount: '',
  bankAccountDigit: '',
  bankAccountType: 'CORRENTE',
  acceptedTermVersion: '',
  acceptedTerm: false,
};

@Component({
  selector: 'app-reembolso-hub',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './reembolso-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReembolsoHub {
  private readonly api = inject(ReimbursementApi);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly formatBrl = formatBrl;
  protected readonly formatBrDate = formatBrDate;
  protected readonly statusBadge = statusBadge;
  protected readonly tabs: ReembolsoTab[] = ['solicitar', 'historico', 'extrato', 'previas'];

  protected readonly tab = signal<ReembolsoTab>('solicitar');
  protected readonly loading = signal(true);
  protected readonly eligible = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly catalog = signal<CatalogView | null>(null);
  protected readonly termBody = signal('');
  protected readonly history = signal<ReimbursementHistoryItem[]>([]);
  protected readonly detail = signal<ReimbursementDetailView | null>(null);
  protected readonly statement = signal<ReimbursementStatementView | null>(null);
  protected readonly previews = signal<ReimbursementPreviewListItem[]>([]);
  protected readonly success = signal<ReimbursementSubmissionResult | null>(null);
  protected readonly previewResult = signal<ReimbursementPreviewResult | null>(null);
  protected readonly submitErrorKey = signal<string | null>(null);
  protected readonly uploadErrorKey = signal<string | null>(null);
  protected readonly submitting = signal(false);

  protected form: RequestForm = { ...EMPTY_FORM };
  protected preview = { beneficiaryId: '', expenseTypeCode: 'CONSULTA' };
  protected statementFrom = '2026-06-01';
  protected statementTo = '2026-06-30';
  protected readonly requestFiles = signal<UploadedFileEntry[]>([]);
  protected readonly pendencyFiles = signal<UploadedFileEntry[]>([]);
  protected readonly previewFiles = signal<UploadedFileEntry[]>([]);

  constructor() {
    effect(() => {
      const active = this.context.active();
      if (!active) {
        return;
      }
      this.form.beneficiaryId = active.beneficiaryId;
      this.preview.beneficiaryId = active.beneficiaryId;
    });
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.eligibility().subscribe({
      next: (gate) => {
        this.eligible.set(gate.eligible);
        if (!gate.eligible) {
          this.loading.set(false);
          return;
        }
        this.loadEligibleData();
      },
      error: () => {
        this.loading.set(false);
        this.errorKey.set('common.error');
      },
    });
  }

  selectTab(tab: ReembolsoTab): void {
    this.tab.set(tab);
    if (tab === 'historico') {
      this.loadHistory();
    }
    if (tab === 'extrato') {
      this.loadStatement();
    }
    if (tab === 'previas') {
      this.loadPreviews();
    }
  }

  addSession(): void {
    this.form.sessions = [...this.form.sessions, { sessionDate: this.form.careDate, amount: 0 }];
  }

  removeSession(index: number): void {
    this.form.sessions = this.form.sessions.filter((_, i) => i !== index);
  }

  async addRequestFile(event: Event, category: DocumentCategory): Promise<void> {
    await this.addFile(event, category, this.requestFiles);
  }

  async addPendencyFile(event: Event, category: DocumentCategory): Promise<void> {
    await this.addFile(event, category, this.pendencyFiles);
  }

  async addPreviewFile(event: Event, category: DocumentCategory): Promise<void> {
    await this.addFile(event, category, this.previewFiles);
  }

  removeFile(list: WritableSignal<UploadedFileEntry[]>, index: number): void {
    list.set(list().filter((_, i) => i !== index));
  }

  submit(): void {
    this.submitErrorKey.set(null);
    this.success.set(null);
    if (!this.form.acceptedTerm) {
      this.submitErrorKey.set('reembolso.erro.termo');
      return;
    }
    this.submitting.set(true);
    this.api
      .submit(
        { ...this.form, acceptedTermVersion: this.form.acceptedTermVersion },
        this.requestFiles(),
        crypto.randomUUID(),
      )
      .subscribe({
        next: (result) => {
          this.success.set(result);
          this.submitting.set(false);
          this.loadHistory();
        },
        error: (error: HttpErrorResponse) => {
          this.submitting.set(false);
          this.submitErrorKey.set(error.error?.code ?? 'common.error');
        },
      });
  }

  selectDetail(id: string): void {
    this.api.detail(id).subscribe({
      next: (detail) => this.detail.set(detail),
      error: () => this.submitErrorKey.set('common.error'),
    });
  }

  resolvePendency(): void {
    const current = this.detail();
    if (!current) {
      return;
    }
    this.api.resolvePendency(current.id, this.pendencyFiles()).subscribe({
      next: () => {
        this.pendencyFiles.set([]);
        this.selectDetail(current.id);
        this.loadHistory();
      },
      error: (error: HttpErrorResponse) => this.submitErrorKey.set(error.error?.code ?? 'common.error'),
    });
  }

  correctBank(): void {
    const current = this.detail();
    if (!current) {
      return;
    }
    this.api.correctBank(current.id, this.form).subscribe({
      next: () => {
        this.selectDetail(current.id);
        this.loadHistory();
        this.loadStatement();
      },
      error: (error: HttpErrorResponse) => this.submitErrorKey.set(error.error?.code ?? 'common.error'),
    });
  }

  loadStatement(): void {
    this.api.statement(this.statementFrom, this.statementTo).subscribe({
      next: (statement) => this.statement.set(statement),
      error: () => this.submitErrorKey.set('common.error'),
    });
  }

  createPreview(): void {
    this.previewResult.set(null);
    this.api.createPreview(this.preview, this.previewFiles()).subscribe({
      next: (result) => {
        this.previewResult.set(result);
        this.previewFiles.set([]);
        this.loadPreviews();
      },
      error: (error: HttpErrorResponse) => this.submitErrorKey.set(error.error?.code ?? 'common.error'),
    });
  }

  private loadEligibleData(): void {
    this.api.catalog().subscribe({
      next: (catalog) => {
        this.catalog.set(catalog);
        this.form.providerCouncilCode = catalog.councils[0]?.code ?? 'CRM';
        this.form.bankCode = catalog.banks[0]?.code ?? '001';
      },
      error: () => this.errorKey.set('common.error'),
    });
    this.api.term().subscribe({
      next: (term) => {
        this.form.acceptedTermVersion = term.version;
        this.termBody.set(term.body);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorKey.set('common.error');
      },
    });
    this.loadHistory();
    this.loadStatement();
    this.loadPreviews();
  }

  private loadHistory(): void {
    this.api.history().subscribe({
      next: (items) => {
        this.history.set(items);
        if (!this.detail() && items[0]) {
          this.selectDetail(items[0].id);
        }
      },
      error: () => this.submitErrorKey.set('common.error'),
    });
  }

  private loadPreviews(): void {
    this.api.previews().subscribe({
      next: (items) => this.previews.set(items),
      error: () => this.submitErrorKey.set('common.error'),
    });
  }

  private async addFile(
    event: Event,
    category: DocumentCategory,
    list: WritableSignal<UploadedFileEntry[]>,
  ): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    const error = await validateReimbursementFile(file, totalBytes(list()));
    if (error) {
      this.uploadErrorKey.set(error);
      return;
    }
    this.uploadErrorKey.set(null);
    list.set([...list(), { category, file }]);
  }
}
