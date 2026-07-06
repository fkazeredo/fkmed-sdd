import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ClinicalDocumentDetail, ClinicalDocumentsApi } from './clinical-documents.api';
import { formatBrDate, validityStatus } from './document-format';

/**
 * Clinical document detail (SPEC-0011 BR6): common header (type, issue date, professional+CRM,
 * beneficiary, validity) plus a type-specific body rendered via `@switch (doc.type)`. "Baixar
 * PDF" (BR7) stays available even on an expired document (BR5). The referral body's "Agendar
 * consulta" (BR6/AC4) hands the target specialty off to the SPEC-0009 wizard via a query param —
 * `especialidade`, read by `ConsultaWizard.ngOnInit` (features/agendamento/consulta-wizard.ts).
 * A missing/out-of-scope id renders a dedicated "não encontrado" state (404
 * `document.not-found`), distinct from a transient/unexpected failure (BR9 existence not
 * revealed).
 */
@Component({
  selector: 'app-document-detail',
  imports: [TranslatePipe],
  templateUrl: './document-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocumentDetail implements OnInit {
  private readonly api = inject(ClinicalDocumentsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly doc = signal<ClinicalDocumentDetail | null>(null);

  protected readonly downloading = signal(false);
  protected readonly pdfErrorKey = signal<string | null>(null);

  protected readonly validityStatus = validityStatus;
  protected readonly formatBrDate = formatBrDate;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.loading.set(true);
    this.notFound.set(false);
    this.errorKey.set(null);
    this.api.getDocument(id).subscribe({
      next: (detail) => {
        this.doc.set(detail);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.error?.code === 'document.not-found') {
          this.notFound.set(true);
        } else {
          this.errorKey.set('common.error');
        }
      },
    });
  }

  downloadPdf(): void {
    const doc = this.doc();
    if (!doc || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    this.pdfErrorKey.set(null);
    this.api.downloadPdf(doc.id).subscribe({
      next: (blob) => {
        this.downloading.set(false);
        this.triggerDownload(blob, `documento-${doc.id}.pdf`);
      },
      error: () => {
        this.downloading.set(false);
        this.pdfErrorKey.set('minhaSaude.erro.pdf');
      },
    });
  }

  /** BR6/AC4: the referral's target specialty code is passed as-is — the wizard's specialty step
   * (features/agendamento/consulta-wizard.ts) resolves it against its own loaded specialty list. */
  agendarConsulta(): void {
    const doc = this.doc();
    if (!doc?.specialtyCode) {
      return;
    }
    void this.router.navigate(['/agendamento/consulta'], { queryParams: { especialidade: doc.specialtyCode } });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
