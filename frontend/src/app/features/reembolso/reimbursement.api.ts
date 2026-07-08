import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type ReimbursementStatus =
  | 'EM_ANALISE'
  | 'PROCESSAMENTO'
  | 'PENDENTE_DOCUMENTACAO'
  | 'APROVADO'
  | 'PAGO'
  | 'PAGAMENTO_NAO_EFETUADO'
  | 'NEGADO'
  | 'CANCELADO';

export type PreviewSituation = 'EM_ANALISE' | 'CONCLUIDA';
export type DocumentCategory = 'RECEIPT' | 'MEDICAL_ORDER' | 'COMPLEMENTARY' | 'BUDGET';
export type BankAccountType = 'CORRENTE' | 'POUPANCA';

export interface EligibilityView {
  eligible: boolean;
}

export interface AdhesionTermView {
  version: string;
  publishedAt: string;
  body: string;
}

export interface CatalogView {
  expenseTypes: { code: string; name: string }[];
  councils: { code: string; name: string }[];
  banks: { code: string; name: string }[];
}

export interface UploadedFileEntry {
  category: DocumentCategory;
  file: File;
}

export interface SessionInput {
  sessionDate: string;
  amount: number;
}

export interface SubmitReimbursementPayload {
  beneficiaryId: string;
  expenseTypeCode: string;
  careDate: string;
  amount: number;
  sessions: SessionInput[];
  providerName: string;
  providerCouncilCode: string;
  providerCouncilNumber: string;
  providerCouncilUf: string;
  providerDocument: string;
  providerSpecialty: string;
  bankCode: string;
  bankAgency: string;
  bankAccount: string;
  bankAccountDigit: string;
  bankAccountType: BankAccountType;
  acceptedTermVersion: string;
}

export interface ReimbursementSubmissionResult {
  protocol: string;
  status: ReimbursementStatus;
  expectedPaymentDate: string;
}

export interface ReimbursementActionResult {
  id: string;
  protocol: string;
  status: ReimbursementStatus;
  expectedPaymentDate: string;
}

export interface ReimbursementHistoryItem {
  id: string;
  protocol: string;
  expenseType: string;
  beneficiary: string;
  requestedAt: string;
  amountRequested: number;
  amountReimbursed?: number;
  status: ReimbursementStatus;
}

export interface ReimbursementDetailView extends ReimbursementHistoryItem {
  careDate: string;
  expectedPaymentDate: string;
  glosa?: { amount: number; reason: string };
  denialReason?: string;
  pendency?: { description: string; deadlineAt: string };
  bank: { bankCode: string; agency: string; account: string; type: BankAccountType };
  provider: { name: string; councilCode: string; councilNumber: string; councilUf: string; specialty: string };
  documents: { category: DocumentCategory; fileName: string; fileSize: number; uploadedAt: string }[];
  timeline: { occurredAt: string; status: ReimbursementStatus; description: string }[];
  regulatoryNote: string;
}

export interface ReimbursementStatementView {
  items: { id: string; protocol: string; beneficiary: string; paidAt: string; amountPaid: number }[];
  total: number;
}

export interface PreviewPayload {
  beneficiaryId: string;
  expenseTypeCode: string;
}

export interface ReimbursementPreviewResult {
  id: string;
  protocol: string;
  expenseType: string;
  beneficiary: string;
  situation: PreviewSituation;
  estimatedValue?: number;
  createdAt: string;
  concludedAt?: string;
  base: string;
  disclaimer: string;
}

export interface ReimbursementPreviewListItem {
  id: string;
  protocol: string;
  expenseType: string;
  beneficiary: string;
  requestedAt: string;
  situation: PreviewSituation;
  estimatedValue?: number;
}

@Injectable({ providedIn: 'root' })
export class ReimbursementApi {
  private readonly http = inject(HttpClient);

  eligibility(): Observable<EligibilityView> {
    return this.http.get<EligibilityView>('/api/reimbursements/eligibility');
  }

  term(): Observable<AdhesionTermView> {
    return this.http.get<AdhesionTermView>('/api/reimbursements/term');
  }

  catalog(): Observable<CatalogView> {
    return this.http.get<CatalogView>('/api/reimbursements/catalog');
  }

  history(): Observable<ReimbursementHistoryItem[]> {
    return this.http.get<ReimbursementHistoryItem[]>('/api/reimbursements');
  }

  detail(id: string): Observable<ReimbursementDetailView> {
    return this.http.get<ReimbursementDetailView>(`/api/reimbursements/${id}`);
  }

  statement(from?: string, to?: string): Observable<ReimbursementStatementView> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<ReimbursementStatementView>('/api/reimbursements/statement', { params });
  }

  submit(
    payload: SubmitReimbursementPayload,
    files: UploadedFileEntry[],
    idempotencyKey: string,
  ): Observable<ReimbursementSubmissionResult> {
    return this.http.post<ReimbursementSubmissionResult>(
      '/api/reimbursements',
      multipart({ ...payload, documents: metadata(files) }, files),
      { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) },
    );
  }

  resolvePendency(id: string, files: UploadedFileEntry[]): Observable<ReimbursementActionResult> {
    return this.http.post<ReimbursementActionResult>(
      `/api/reimbursements/${id}/pendency-documents`,
      multipart({ documents: metadata(files) }, files),
    );
  }

  correctBank(id: string, body: SubmitReimbursementPayload): Observable<ReimbursementActionResult> {
    return this.http.post<ReimbursementActionResult>(`/api/reimbursements/${id}/bank-correction`, {
      bankCode: body.bankCode,
      bankAgency: body.bankAgency,
      bankAccount: body.bankAccount,
      bankAccountDigit: body.bankAccountDigit,
      bankAccountType: body.bankAccountType,
    });
  }

  previews(): Observable<ReimbursementPreviewListItem[]> {
    return this.http.get<ReimbursementPreviewListItem[]>('/api/reimbursement-previews');
  }

  createPreview(payload: PreviewPayload, files: UploadedFileEntry[]): Observable<ReimbursementPreviewResult> {
    return this.http.post<ReimbursementPreviewResult>(
      '/api/reimbursement-previews',
      multipart({ ...payload, documents: metadata(files) }, files),
    );
  }
}

function multipart(request: object, files: UploadedFileEntry[]): FormData {
  const form = new FormData();
  form.append('request', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  for (const item of files) {
    form.append('documents', item.file, item.file.name);
  }
  return form;
}

function metadata(files: UploadedFileEntry[]): { category: DocumentCategory; fileName: string }[] {
  return files.map((item) => ({ category: item.category, fileName: item.file.name }));
}
