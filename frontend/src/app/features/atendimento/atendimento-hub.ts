import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AntifraudContent, SupportApi, SupportChannel } from './support.api';

/** One rendered channel card — one or two contact lines grouped under a shared label (BR1). */
interface ChannelCard {
  type: string;
  label: string;
  lines: { sublabel?: string; value: string; hours?: string }[];
}

/**
 * The Atendimento hub (SPEC-0014): channel cards (BR1/BR2) plus the antifraud section (BR3),
 * anchored at `#antifraude` — the destination of the Home fraud banner (AC2). Links out to the
 * FAQ and Central de Libras screens.
 */
@Component({
  selector: 'app-atendimento-hub',
  imports: [RouterLink, TranslatePipe],
  templateUrl: './atendimento-hub.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AtendimentoHub implements OnInit {
  private readonly api = inject(SupportApi);

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  private readonly channels = signal<SupportChannel[]>([]);
  protected readonly antifraud = signal<AntifraudContent | null>(null);

  /** Groups same-type rows (Central 24h) under one card, in display order (BR1). */
  protected readonly cards = computed<ChannelCard[]>(() => {
    const grouped: ChannelCard[] = [];
    for (const channel of this.channels()) {
      const existing = grouped.find((card) => card.type === channel.type);
      const line = { sublabel: channel.sublabel, value: channel.value, hours: channel.hours };
      if (existing) {
        existing.lines.push(line);
      } else {
        grouped.push({ type: channel.type, label: channel.label, lines: [line] });
      }
    }
    return grouped;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getChannels().subscribe({
      next: (list) => this.channels.set(list),
      error: () => this.errorKey.set('common.error'),
    });
    this.api.getAntifraud().subscribe({
      next: (content) => {
        this.antifraud.set(content);
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }

  /** A `tel:` href keeping only a leading `+` and digits (tap-to-call, BR1). */
  telHref(value: string): string {
    return 'tel:' + value.replace(/(?!^\+)[^\d]/g, '');
  }

  /** A `wa.me` href — digits only, no `+`/spaces/punctuation (BR1/AC5). */
  waHref(value: string): string {
    return 'https://wa.me/' + value.replace(/\D/g, '');
  }
}
