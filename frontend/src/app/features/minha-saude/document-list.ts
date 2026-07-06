import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  ClinicalDocumentCard,
  ClinicalDocumentsApi,
  DocumentCategory,
  DocumentListFilters,
  PeriodOption,
} from './clinical-documents.api';
import { formatBrDate, sortByIssuedAtDesc, validityStatus } from './document-format';

/**
 * Document list per Minha Saúde category (SPEC-0011 BR2/BR4/BR5): one screen serves all 3 hub
 * destinations — the route's `data.categories` (app.routes.ts) says which underlying API
 * category/categories it queries. "Receituários/Atestados" covers 2 wire categories
 * (PRESCRIPTION + SICK_NOTE — there is no combined code, BR1) so its results are 2 requests
 * merged and re-sorted client-side, most-recent-first; the other two screens make a single
 * request and just render the backend's order. Filters: beneficiary (default "todos"/`all`,
 * accessible list from SPEC-0003) and period (30/90/365 days, or a custom range applied
 * explicitly via "Aplicar filtro" once both dates are set).
 */
@Component({
  selector: 'app-document-list',
  imports: [FormsModule, RouterLink, TranslatePipe],
  templateUrl: './document-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentList implements OnInit {
  private readonly api = inject(ClinicalDocumentsApi);
  private readonly route = inject(ActivatedRoute);
  protected readonly context = inject(BeneficiaryContextService);

  protected categories: DocumentCategory[] = [];
  protected readonly titleKey = signal('minhaSaude.hub.title');

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly items = signal<ClinicalDocumentCard[]>([]);

  protected readonly beneficiaryId = signal('all');
  protected readonly period = signal<PeriodOption>('P90D');
  protected readonly customFrom = signal('');
  protected readonly customTo = signal('');

  protected readonly validityStatus = validityStatus;
  protected readonly formatBrDate = formatBrDate;

  ngOnInit(): void {
    const data = this.route.snapshot.data as { categories?: DocumentCategory[]; titleKey?: string };
    this.categories = data.categories ?? [];
    this.titleKey.set(data.titleKey ?? 'minhaSaude.hub.title');
    this.load();
  }

  onBeneficiaryChange(value: string): void {
    this.beneficiaryId.set(value);
    this.load();
  }

  /** Switching to a named period re-queries immediately; switching to "custom" waits for
   * "Aplicar filtro" — both dates are required before there is anything sane to query. */
  onPeriodChange(value: PeriodOption): void {
    this.period.set(value);
    if (value !== 'CUSTOM') {
      this.load();
    }
  }

  applyCustomRange(): void {
    if (!this.customFrom() || !this.customTo()) {
      return;
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    const requests = this.categories.map((category) => this.api.getDocuments(this.buildFilters(category)));
    forkJoin(requests).subscribe({
      next: (responses) => {
        const merged = responses.flatMap((response) => response.items);
        this.items.set(sortByIssuedAtDesc(merged));
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }

  private buildFilters(category: DocumentCategory): DocumentListFilters {
    const beneficiaryId = this.beneficiaryId();
    const period = this.period();
    if (period === 'CUSTOM') {
      return { category, beneficiaryId, from: this.customFrom(), to: this.customTo() };
    }
    return { category, beneficiaryId, period };
  }
}
