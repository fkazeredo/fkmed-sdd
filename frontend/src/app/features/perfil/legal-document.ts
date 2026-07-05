import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LegalApi, LegalDocumentDetail, LegalDocumentType } from '../../core/legal/legal.api';

/**
 * A read-only legal document page (SPEC-0006 BR8): Termos de uso (`type: TERMS`) or Comunicado de
 * privacidade (`type: PRIVACY`), selected by route data. Shows the current text (fetched from
 * `GET /api/legal-documents/{type}`) with its version number and publication date. Reached from
 * the Perfil menu (not the interception flow).
 */
@Component({
  selector: 'app-legal-document',
  imports: [TranslatePipe, DatePipe],
  templateUrl: './legal-document.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegalDocumentPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(LegalApi);

  readonly type = signal<LegalDocumentType>('TERMS');
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly doc = signal<LegalDocumentDetail | null>(null);

  readonly titleKey = computed(() =>
    this.type() === 'TERMS' ? 'perfil.legal.termos.title' : 'perfil.legal.privacidade.title',
  );

  ngOnInit(): void {
    const routeType = (this.route.snapshot.data['type'] as LegalDocumentType | undefined) ?? 'TERMS';
    this.type.set(routeType);
    this.api.getDocument(routeType).subscribe({
      next: (document) => {
        this.doc.set(document);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }
}
