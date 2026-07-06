import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { GuideDetail as GuideDetailModel, GuidesApi } from './guias.api';
import { formatBrDate, GUIDE_STATUS_BADGE, showsAuthPassword } from './guide-format';

/**
 * Guide detail (SPEC-0012 BR5/BR7): items table (TUSS code, description, quantity, item status);
 * when AUTORIZADA/PARCIALMENTE_AUTORIZADA, the authorization password + validity; an expired
 * password (`authExpired`) shows the dedicated notice (BR7); NEGADA shows the denial reason; plus
 * contact guidance. A missing/out-of-scope id renders a dedicated "não encontrada" state (404
 * `guide.not-found`), distinct from a transient/unexpected failure — mirrors DocumentDetail
 * (features/minha-saude/document-detail.ts).
 */
@Component({
  selector: 'app-guide-detail',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './guide-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GuideDetail implements OnInit {
  private readonly api = inject(GuidesApi);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly guide = signal<GuideDetailModel | null>(null);

  protected readonly badgeClass = GUIDE_STATUS_BADGE;
  protected readonly formatBrDate = formatBrDate;
  protected readonly showsAuthPassword = showsAuthPassword;

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
    this.api.getGuide(id).subscribe({
      next: (detail) => {
        this.guide.set(detail);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        if (error.error?.code === 'guide.not-found') {
          this.notFound.set(true);
        } else {
          this.errorKey.set('common.error');
        }
      },
    });
  }
}
