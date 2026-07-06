import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

interface AgendamentoCard {
  key: string;
  icon: string;
  labelKey: string;
  routerLink: string | null;
}

/** SPEC-0009 §Scope: the Agendamento hub — Agendar Consulta, Agendar Exame and Meus Agendamentos
 * are delivered this phase; Telemedicina renders disabled with the "em breve" hint (its booking
 * mechanics are SPEC-0010, mirrors the Rede hub's phased-delivery pattern). */
const AGENDAMENTO_CARDS: AgendamentoCard[] = [
  { key: 'consulta', icon: 'pi pi-user', labelKey: 'agendamento.hub.consulta', routerLink: '/agendamento/consulta' },
  { key: 'exame', icon: 'pi pi-file', labelKey: 'agendamento.hub.exame', routerLink: '/agendamento/exame' },
  { key: 'meus', icon: 'pi pi-calendar', labelKey: 'agendamento.hub.meus', routerLink: '/agendamento/meus' },
  { key: 'telemedicina', icon: 'pi pi-video', labelKey: 'agendamento.hub.telemedicina', routerLink: null },
];

/** The Agendamento hub (SPEC-0009 §Scope): entry point for the scheduling features. */
@Component({
  selector: 'app-agendamento-hub',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './agendamento-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendamentoHub {
  protected readonly cards = AGENDAMENTO_CARDS;
}
