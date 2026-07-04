import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MyPlanResponse, PlanApi } from './plan-api';

/**
 * The Meu Plano screen (SPEC-0001 BR6): plan data and family members of the logged-in
 * beneficiary, fetched from the API — nothing hardcoded. Handles loading/error/success
 * states (docs/specs/README.md §UI norms).
 */
@Component({
  selector: 'app-my-plan',
  imports: [TranslatePipe, TableModule, TagModule],
  templateUrl: './my-plan.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPlan implements OnInit {
  private readonly api = inject(PlanApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly data = signal<MyPlanResponse | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.api.getMyPlan().subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** Coverage is a backend code (e.g. ESTADUAL); the label always comes from the bundle. */
  coverageLabel(code: string): string {
    return this.translate.instant(`meuPlano.coverage.${code}`);
  }

  roleLabel(role: string): string {
    return this.translate.instant(`meuPlano.role.${role}`);
  }
}
