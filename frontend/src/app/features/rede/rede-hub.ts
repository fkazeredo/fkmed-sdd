import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

interface RedeCard {
  key: string;
  icon: string;
  labelKey: string;
  routerLink: string | null;
}

/** SPEC-0008 §Scope: only "Busca de rede" is delivered in this phase — Agendamento (own-network,
 * SPEC-0009), Telemedicina and Minha Saúde render disabled with the "em breve" hint (mirrors
 * Home's phased-delivery pattern, SPEC-0005). */
const REDE_CARDS: RedeCard[] = [
  { key: 'buscaDeRede', icon: 'pi pi-map', labelKey: 'rede.hub.buscaDeRede', routerLink: '/rede/busca' },
  { key: 'agendamento', icon: 'pi pi-calendar', labelKey: 'rede.hub.agendamento', routerLink: null },
  { key: 'telemedicina', icon: 'pi pi-video', labelKey: 'rede.hub.telemedicina', routerLink: null },
  { key: 'minhaSaude', icon: 'pi pi-heart', labelKey: 'rede.hub.minhaSaude', routerLink: null },
];

/** The Rede hub (SPEC-0008 §Scope): entry point for the 4 network-related features. */
@Component({
  selector: 'app-rede-hub',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './rede-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RedeHub {
  protected readonly cards = REDE_CARDS;
}
