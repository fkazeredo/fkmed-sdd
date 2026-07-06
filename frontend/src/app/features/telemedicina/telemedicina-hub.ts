import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TeleInstabilityBanner } from './tele-instability-banner';

interface TeleCard {
  key: string;
  icon: string;
  labelKey: string;
  descriptionKey: string;
  routerLink: string;
}

/** SPEC-0010 BR1: the three telemedicine entry points. Pronto Atendimento (24/7 virtual queue),
 * Agendar Consulta (scheduled teleconsultation) and Meus Agendamentos (filtered to Telemedicina). */
const TELE_CARDS: TeleCard[] = [
  {
    key: 'pronto',
    icon: 'pi pi-bolt',
    labelKey: 'telemedicina.hub.pronto',
    descriptionKey: 'telemedicina.hub.prontoDescricao',
    routerLink: '/telemedicina/triagem',
  },
  {
    key: 'agendar',
    icon: 'pi pi-calendar-plus',
    labelKey: 'telemedicina.hub.agendar',
    descriptionKey: 'telemedicina.hub.agendarDescricao',
    routerLink: '/telemedicina/agendar',
  },
  {
    key: 'meus',
    icon: 'pi pi-video',
    labelKey: 'telemedicina.hub.meus',
    descriptionKey: 'telemedicina.hub.meusDescricao',
    routerLink: '/telemedicina/agendamentos',
  },
];

/** The Telemedicine hub (SPEC-0010 BR1): entry point + instability banner (AC7). */
@Component({
  selector: 'app-telemedicina-hub',
  imports: [RouterLink, TranslatePipe, TeleInstabilityBanner],
  templateUrl: './telemedicina-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TelemedicinaHub {
  protected readonly cards = TELE_CARDS;
}
