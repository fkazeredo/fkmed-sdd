import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Derived invoice status (SPEC-0013 BR2). FROZEN contract: docs/api/openapi.json. */
export type InvoiceStatus = 'OPEN' | 'OVERDUE' | 'PAID';

/** The two invoice tabs (BR2): OPEN groups open + overdue, PAID lists paid ones. */
export type InvoiceTab = 'OPEN' | 'PAID';

/** Copay period filters (BR5); CUSTOM sends `from`/`to` instead of `period`. */
export type CopayPeriod = 'CURRENT_MONTH' | 'LAST_3M' | 'CUSTOM';

/** The overdue update on the ORIGINAL amount (BR2/OQ1): multa 2% + juros 1%/mês pro rata die. */
export interface UpdatedAmount {
  original: number;
  fine: number;
  interest: number;
  daysOverdue: number;
  total: number;
}

/** A row of the invoice list (BR2). `paidAt` only on PAID; `updatedAmount` only on OVERDUE. */
export interface InvoiceSummary {
  id: string;
  competencia: string;
  dueDate: string;
  amount: number;
  status: InvoiceStatus;
  paidAt?: string;
  updatedAmount?: UpdatedAmount;
}

/** The invoice detail (BR3): summary + the payment identifiers. */
export interface InvoiceDetail extends InvoiceSummary {
  digitableLine: string;
  pixCode: string;
  barcodePayload: string;
}

/** The antifraud validator verdict (BR4). NOT_RECOGNIZED carries no invoice data. */
export interface InvoiceValidation {
  result: 'AUTHENTIC' | 'NOT_RECOGNIZED';
  competencia?: string;
  dueDate?: string;
  amount?: number;
}

/** One copay row (BR5). */
export interface CopayLine {
  date: string;
  procedure: string;
  provider: string;
  beneficiaryName: string;
  amount: number;
}

/** The copay statement for a filter (BR5): entries + their total. */
export interface CopayStatement {
  entries: CopayLine[];
  total: number;
}

/** A base year offered for an IR statement or a settlement declaration (BR6/BR7). */
export interface StatementYear {
  year: number;
}

/** Filters for the copay statement. */
export interface CopayFilters {
  period: CopayPeriod;
  from?: string;
  to?: string;
  beneficiaryId?: string;
}

/**
 * Domain-oriented API of the Finanças feature (SPEC-0013), built against the FROZEN contract
 * (docs/api/openapi.json) — every route is titular-only (BR1) and enforced server-side. No raw
 * HttpClient in components (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class FinanceApi {
  private readonly http = inject(HttpClient);

  /** BR2: the OPEN (open + overdue) or PAID tab of the titular's invoices. */
  getInvoices(tab: InvoiceTab): Observable<InvoiceSummary[]> {
    return this.http.get<InvoiceSummary[]>('/api/finance/invoices', {
      params: new HttpParams().set('tab', tab),
    });
  }

  /** BR3: the invoice detail with digitable line, PIX code and barcode. */
  getInvoice(id: string): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(`/api/finance/invoices/${id}`);
  }

  /** BR3: the second-copy PDF (a paid invoice carries the "PAGO" watermark). */
  downloadInvoicePdf(id: string): Observable<Blob> {
    return this.http.get(`/api/finance/invoices/${id}/pdf`, { responseType: 'blob' });
  }

  /** BR4: validate a submitted line (normalized + 47-digit checked server-side). */
  validate(line: string): Observable<InvoiceValidation> {
    return this.http.post<InvoiceValidation>('/api/finance/invoices/validate', { line });
  }

  /** BR5: the copay statement for the period (and optional single beneficiary) with its total. */
  getCopay(filters: CopayFilters): Observable<CopayStatement> {
    let params = new HttpParams().set('period', filters.period);
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.beneficiaryId) {
      params = params.set('beneficiaryId', filters.beneficiaryId);
    }
    return this.http.get<CopayStatement>('/api/finance/copay', { params });
  }

  /** BR6: the base years with payments. */
  getTaxStatements(): Observable<StatementYear[]> {
    return this.http.get<StatementYear[]>('/api/finance/tax-statements');
  }

  /** BR6: the IR statement PDF for a base year. */
  downloadTaxStatementPdf(year: number): Observable<Blob> {
    return this.http.get(`/api/finance/tax-statements/${year}/pdf`, { responseType: 'blob' });
  }

  /** BR7: the fully-paid base years (Lei 12.007). */
  getSettlementDeclarations(): Observable<StatementYear[]> {
    return this.http.get<StatementYear[]>('/api/finance/settlement-declarations');
  }

  /** BR7: the settlement declaration PDF for a fully-paid year. */
  downloadSettlementPdf(year: number): Observable<Blob> {
    return this.http.get(`/api/finance/settlement-declarations/${year}/pdf`, { responseType: 'blob' });
  }
}
