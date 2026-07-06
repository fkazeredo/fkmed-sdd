import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * The friendly denial screen shown when a dependent reaches any Finanças route (SPEC-0013 BR1).
 * Finance is exclusive to the contract titular; when the active beneficiary is a dependent the nav
 * entry is hidden (shell) and every finance screen renders this instead of its content — no error,
 * a clear explanation and a way back to Início.
 */
@Component({
  selector: 'app-finance-denied',
  imports: [TranslatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="mx-auto max-w-xl p-6 text-center"
      aria-labelledby="finance-denied-title"
      data-testid="financas-denied"
    >
      <i class="pi pi-lock mb-3 text-4xl text-teal-700" aria-hidden="true"></i>
      <h1 id="finance-denied-title" class="mb-2 text-2xl font-semibold text-teal-800">
        {{ 'financas.negado.title' | translate }}
      </h1>
      <p class="mb-5 text-slate-600">{{ 'financas.negado.mensagem' | translate }}</p>
      <a
        routerLink="/home"
        class="inline-block rounded-lg bg-teal-700 px-4 py-2 text-white hover:bg-teal-800"
        data-testid="financas-denied-home"
      >
        {{ 'financas.negado.voltar' | translate }}
      </a>
    </section>
  `,
})
export class FinanceDenied {}
