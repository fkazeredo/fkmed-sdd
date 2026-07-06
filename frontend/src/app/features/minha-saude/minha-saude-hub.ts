import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

interface MinhaSaudeCard {
  key: string;
  icon: string;
  labelKey: string;
  routerLink: string;
}

/** SPEC-0011 BR1: the 3 categories, all delivered in this slice (no phased "em breve" card —
 * unlike the Rede/Agendamento hubs, every card here has a working destination). */
const MINHA_SAUDE_CARDS: MinhaSaudeCard[] = [
  { key: 'exames', icon: 'pi pi-file', labelKey: 'minhaSaude.hub.exames', routerLink: '/minha-saude/exames' },
  {
    key: 'encaminhamentos',
    icon: 'pi pi-directions',
    labelKey: 'minhaSaude.hub.encaminhamentos',
    routerLink: '/minha-saude/encaminhamentos',
  },
  {
    key: 'receituarios',
    icon: 'pi pi-book',
    labelKey: 'minhaSaude.hub.receituarios',
    routerLink: '/minha-saude/receituarios',
  },
];

/** The Minha Saúde hub (SPEC-0011 BR1): entry point for the 3 clinical-document categories. */
@Component({
  selector: 'app-minha-saude-hub',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './minha-saude-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MinhaSaudeHub {
  protected readonly cards = MINHA_SAUDE_CARDS;
}
