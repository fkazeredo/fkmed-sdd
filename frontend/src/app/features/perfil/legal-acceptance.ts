import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LegalApi, LegalDocument, LegalDocumentType } from '../../core/legal/legal.api';
import { LegalDocumentsService } from '../../core/legal/legal-documents.service';

interface PendingDocument {
  type: LegalDocumentType;
  doc: LegalDocument;
}

/**
 * Legal-acceptance interception screen (SPEC-0006 BR8, AC5): shown by the CanActivateChild guard
 * while a new mandatory version is unaccepted. It presents each pending document (text + version +
 * publication date) with a "Li e aceito" button; only Sair (the shell header) is otherwise
 * reachable — the guard bounces every other route back here. Once the last pending document is
 * accepted, navigation resumes and the user is sent Home. A 409 (a newer version was published
 * meanwhile) reloads the snapshot and shows the outdated message.
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

  readonly pending = computed<PendingDocument[]>(() => {
    const snapshot = this.legal.current();
    if (!snapshot) {
      return [];
    }
    const list: PendingDocument[] = [];
    if (!snapshot.terms.acceptedByMe) {
      list.push({ type: 'TERMS', doc: snapshot.terms });
    }
    if (!snapshot.privacy.acceptedByMe) {
      list.push({ type: 'PRIVACY', doc: snapshot.privacy });
    }
    return list;
  });

  ngOnInit(): void {
    this.legal.ensureLoaded().subscribe({
      next: () => {
        this.loading.set(false);
        this.redirectIfDone();
      },
      error: () => this.loading.set(false),
    });
  }

  titleKey(type: LegalDocumentType): string {
    return type === 'TERMS' ? 'perfil.legal.termos.title' : 'perfil.legal.privacidade.title';
  }

  accept(type: LegalDocumentType): void {
    if (this.accepting()) {
      return;
    }
    this.accepting.set(type);
    this.errorKey.set(null);
    this.api.accept(type).subscribe({
      next: () => {
        this.legal.markAccepted(type);
        this.accepting.set(null);
        this.redirectIfDone();
      },
      error: (error: HttpErrorResponse) => {
        this.accepting.set(null);
        if (error.status === 409) {
          this.errorKey.set('legal.version-outdated');
          this.legal.reload().subscribe();
        } else {
          this.errorKey.set('common.error');
        }
      },
    });
  }

  private redirectIfDone(): void {
    if (!this.legal.hasPending()) {
      this.router.navigateByUrl('/home');
    }
  }
}
