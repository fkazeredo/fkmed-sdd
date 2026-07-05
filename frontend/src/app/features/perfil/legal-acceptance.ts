import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { LegalApi, LegalDocumentDetail, LegalDocumentType } from '../../core/legal/legal.api';
import { LegalDocumentsService } from '../../core/legal/legal-documents.service';

/**
 * Legal-acceptance interception screen (SPEC-0006 BR8, AC5): shown by the CanActivateChild guard
 * while a new mandatory version is unaccepted. From the cached `current` snapshot it knows which
 * documents are pending, fetches each one's text (GET /api/legal-documents/{type}) and presents it
 * (text + version + publication date) with a "Li e aceito" button. Only Sair (the shell header) is
 * otherwise reachable — the guard bounces every other route back here. Once the last pending
 * document is accepted, navigation resumes and the user is sent Home. Acceptance sends the version
 * shown; a 409 (a newer version was published meanwhile) reloads the snapshot and shows the
 * outdated message.
 */
@Component({
  selector: 'app-legal-acceptance',
  imports: [TranslatePipe, DatePipe],
  templateUrl: './legal-acceptance.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalAcceptance implements OnInit {
  private readonly legal = inject(LegalDocumentsService);
  private readonly api = inject(LegalApi);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly accepting = signal<LegalDocumentType | null>(null);
  readonly errorKey = signal<string | null>(null);
  readonly pendingDocs = signal<LegalDocumentDetail[]>([]);

  ngOnInit(): void {
    this.legal.ensureLoaded().subscribe({
      next: () => this.loadPendingDocs(),
      error: () => this.loading.set(false),
    });
  }

  titleKey(type: LegalDocumentType): string {
    return type === 'TERMS' ? 'perfil.legal.termos.title' : 'perfil.legal.privacidade.title';
  }

  accept(type: LegalDocumentType, version: string): void {
    if (this.accepting()) {
      return;
    }
    this.accepting.set(type);
    this.errorKey.set(null);
    this.api.accept(type, version).subscribe({
      next: () => {
        this.legal.markAccepted(type);
        this.pendingDocs.update((list) => list.filter((doc) => doc.type !== type));
        this.accepting.set(null);
        this.redirectIfDone();
      },
      error: (error: HttpErrorResponse) => {
        this.accepting.set(null);
        if (error.status === 409) {
          this.errorKey.set('legal.version-outdated');
          // A newer version exists: reload the snapshot and re-fetch the pending texts to accept.
          this.legal.reload().subscribe({ next: () => this.loadPendingDocs() });
        } else {
          this.errorKey.set('common.error');
        }
      },
    });
  }

  private loadPendingDocs(): void {
    const types = this.legal.pendingTypes();
    if (types.length === 0) {
      this.pendingDocs.set([]);
      this.loading.set(false);
      this.redirectIfDone();
      return;
    }
    forkJoin(types.map((type) => this.api.getDocument(type))).subscribe({
      next: (docs) => {
        this.pendingDocs.set(docs);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private redirectIfDone(): void {
    if (!this.legal.hasPending()) {
      this.router.navigateByUrl('/home');
    }
  }
}
