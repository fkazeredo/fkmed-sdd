import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LegalDocumentsService } from '../../core/legal/legal-documents.service';
import { LegalDocumentType } from '../../core/legal/legal.api';

/**
 * A read-only legal document page (SPEC-0006 BR8): Termos de uso (`type: TERMS`) or Comunicado de
 * privacidade (`type: PRIVACY`), selected by route data. Shows the current text with its version
 * number and publication date. Reached from the Perfil menu (not the interception flow).
 */
@Component({
  selector: 'app-legal-document',
  imports: [TranslatePipe, DatePipe],
  templateUrl: './legal-document.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalDocumentPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly legal = inject(LegalDocumentsService);

  readonly type = signal<LegalDocumentType>('TERMS');
  readonly loading = signal(true);
  readonly loadError = signal(false);

  readonly doc = computed(() => {
    const snapshot = this.legal.current();
    if (!snapshot) {
      return null;
    }
    return this.type() === 'TERMS' ? snapshot.terms : snapshot.privacy;
  });

  readonly titleKey = computed(() =>
    this.type() === 'TERMS' ? 'perfil.legal.termos.title' : 'perfil.legal.privacidade.title',
  );

  ngOnInit(): void {
    const routeType = this.route.snapshot.data['type'] as LegalDocumentType | undefined;
    this.type.set(routeType ?? 'TERMS');
    this.legal.ensureLoaded().subscribe({
      next: () => this.loading.set(false),
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }
}
