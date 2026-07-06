import { ChangeDetectionStrategy, Component, ElementRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AccordionModule } from 'primeng/accordion';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import {
  AntifraudView,
  FaqCategory,
  FaqQuestionView,
  LibrasRequestResponse,
  SupportApi,
  SupportChannelView,
} from './support.api';

/** BR5: the six fixed FAQ categories, in the order the spec lists them; `null` is "Todas". */
const FAQ_CATEGORIES: { code: FaqCategory | null; labelKey: string }[] = [
  { code: null, labelKey: 'atendimento.faq.categoria.todas' },
  { code: 'REEMBOLSO', labelKey: 'atendimento.faq.categoria.REEMBOLSO' },
  { code: 'CARTEIRINHA', labelKey: 'atendimento.faq.categoria.CARTEIRINHA' },
  { code: 'AGENDAMENTO', labelKey: 'atendimento.faq.categoria.AGENDAMENTO' },
  { code: 'TELEMEDICINA', labelKey: 'atendimento.faq.categoria.TELEMEDICINA' },
  { code: 'BOLETOS', labelKey: 'atendimento.faq.categoria.BOLETOS' },
  { code: 'REDE', labelKey: 'atendimento.faq.categoria.REDE' },
];

/**
 * BR3: the antifraud practice that links to the SPEC-0013 invoice validator (forward link — the
 * finance route does not exist yet on this branch). Matched by exact text since the frozen
 * `bestPractices` contract is a plain `string[]` with no link metadata; the antifraud copy is fixed
 * seeded content (V25), so this coupling is stable.
 */
const VALIDATE_INVOICE_PRACTICE = 'Valide sempre o boleto antes de pagar.';

/**
 * The Canais de Atendimento screen (SPEC-0014): channel cards (BR1/BR2), the antifraud section
 * (BR3, anchor `#antifraude` — the Home fraud banner's destination, closing SPEC-0005 AC6), the
 * Central de Libras service request (BR4) and the FAQ (BR5/BR6). One page, four sections — the
 * anchor scrolls within this same component rather than navigating to a sub-route.
 */
@Component({
  selector: 'app-atendimento',
  imports: [FormsModule, RouterLink, TranslatePipe, AccordionModule],
  templateUrl: './atendimento.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Atendimento implements OnInit {
  private readonly api = inject(SupportApi);
  private readonly route = inject(ActivatedRoute);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly validateInvoicePractice = VALIDATE_INVOICE_PRACTICE;
  protected readonly faqCategories = FAQ_CATEGORIES;

  // Channels (BR1/BR2).
  protected readonly channelsLoading = signal(true);
  protected readonly channels = signal<SupportChannelView[]>([]);

  // Antifraud (BR3).
  protected readonly antifraud = signal<AntifraudView | null>(null);

  // FAQ (BR5/BR6).
  protected readonly faqTerm = signal('');
  protected readonly faqCategory = signal<FaqCategory | null>(null);
  protected readonly faqResults = signal<FaqQuestionView[]>([]);
  protected readonly faqLoading = signal(false);
  protected readonly openFaqId = signal<string | null>(null);

  // Central de Libras (BR4).
  protected readonly librasSubmitting = signal(false);
  protected readonly librasResult = signal<LibrasRequestResponse | null>(null);
  protected readonly librasError = signal(false);

  ngOnInit(): void {
    this.channelsLoading.set(true);
    this.api.getChannels().subscribe({
      next: (channels) => {
        this.channels.set(channels);
        this.channelsLoading.set(false);
      },
      error: () => this.channelsLoading.set(false),
    });

    this.api.getAntifraud().subscribe((view) => this.antifraud.set(view));

    this.loadFaq();

    // AC2/SPEC-0005 AC6: land positioned at the antifraud section when reached via the Home
    // banner's "Saiba mais" (destination `/atendimento#antifraude`).
    this.route.fragment.subscribe((fragment) => {
      if (fragment) {
        queueMicrotask(() => this.scrollToFragment(fragment));
      }
    });
  }

  /** BR1: tel:-scheme phones need only the digits (kept in full text alongside for desktop use). */
  telHref(value: string): string {
    return `tel:${value.replace(/\D/g, '')}`;
  }

  /** BR1: opens the official WhatsApp number chat — the digits of the stored value. */
  whatsappHref(value: string): string {
    return `https://wa.me/${value.replace(/\D/g, '')}`;
  }

  onFaqTermChange(term: string): void {
    this.faqTerm.set(term);
    this.loadFaq();
  }

  onFaqCategoryChange(category: FaqCategory | null): void {
    this.faqCategory.set(category);
    this.loadFaq();
  }

  onFaqValueChange(value: string | number | string[] | number[] | null | undefined): void {
    this.openFaqId.set(typeof value === 'string' ? value : null);
  }

  requestLibras(): void {
    const beneficiaryId = this.context.active()?.beneficiaryId;
    if (!beneficiaryId || this.librasSubmitting()) {
      return;
    }
    this.librasSubmitting.set(true);
    this.librasError.set(false);
    this.api.requestLibras(beneficiaryId).subscribe({
      next: (response) => {
        this.librasResult.set(response);
        this.librasSubmitting.set(false);
      },
      error: () => {
        this.librasError.set(true);
        this.librasSubmitting.set(false);
      },
    });
  }

  private loadFaq(): void {
    this.faqLoading.set(true);
    this.api.getFaq(this.faqCategory(), this.faqTerm() || null).subscribe({
      next: (results) => {
        this.faqResults.set(results);
        this.faqLoading.set(false);
      },
      error: () => this.faqLoading.set(false),
    });
  }

  private scrollToFragment(fragment: string): void {
    const target = this.elementRef.nativeElement.querySelector(`#${fragment}`);
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
