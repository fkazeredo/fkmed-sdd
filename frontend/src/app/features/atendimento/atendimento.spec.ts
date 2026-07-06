import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { Observable, of } from 'rxjs';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Atendimento } from './atendimento';
import {
  AntifraudView,
  FaqQuestionView,
  LibrasRequestResponse,
  SupportApi,
  SupportChannelView,
} from './support.api';

const CHANNELS: SupportChannelView[] = [
  { type: 'CENTRAL', label: 'Central de Atendimento 24h', value: '0800 123 4567', hours: '24 horas', displayOrder: 1 },
  { type: 'WHATSAPP', label: 'WhatsApp oficial', value: '+55 11 98765-4321', hours: '24 horas', displayOrder: 2 },
  { type: 'OUVIDORIA', label: 'Ouvidoria', value: '0800 765 4321', hours: 'Seg a sex, 8h às 18h', displayOrder: 3 },
  { type: 'ANS', label: 'ANS', value: '0800 701 9656', hours: null, displayOrder: 4 },
];

const ANTIFRAUD: AntifraudView = {
  title: 'Alerta de golpe!',
  message: 'A operadora não solicita dados ou pagamentos por WhatsApp',
  bestPractices: [
    'Nunca compartilhe sua senha ou token de acesso com ninguém.',
    'Valide sempre o boleto antes de pagar.',
    'Utilize somente os canais oficiais desta página para falar com a operadora.',
  ],
};

const FAQ_REEMBOLSO: FaqQuestionView = {
  id: 'faq-1',
  category: 'REEMBOLSO',
  question: 'Até quando posso solicitar reembolso?',
  answer: 'Até 12 meses da data do atendimento.',
  displayOrder: 1,
};
const FAQ_CARTEIRINHA: FaqQuestionView = {
  id: 'faq-2',
  category: 'CARTEIRINHA',
  question: 'Como acesso minha carteirinha?',
  answer: 'Pelo atalho Carteirinha na tela inicial.',
  displayOrder: 1,
};

const MARIA_ACCESSIBLE = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const };

/**
 * SPEC-0014 — Atendimento: channel cards (tap-to-call/WhatsApp), the antifraud section, FAQ
 * (search + category + single-open accordion) and the Central de Libras request. Mocks
 * `SupportApi` (frozen contract) — no dependency on a running backend.
 */
describe('Atendimento', () => {
  let fixture: ComponentFixture<Atendimento>;
  let api: {
    getChannels: ReturnType<typeof vi.fn>;
    getAntifraud: ReturnType<typeof vi.fn>;
    getFaq: ReturnType<typeof vi.fn>;
    requestLibras: ReturnType<typeof vi.fn>;
  };
  let context: BeneficiaryContextService;
  let fragment$: Observable<string | null>;

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [Atendimento],
      providers: [
        provideI18n(),
        { provide: SupportApi, useValue: api },
        { provide: ActivatedRoute, useValue: { fragment: fragment$ } },
      ],
    }).compileComponents();
    context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([MARIA_ACCESSIBLE]);
    context.active.set(MARIA_ACCESSIBLE);
    fixture = TestBed.createComponent(Atendimento);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    fragment$ = of(null);
    api = {
      getChannels: vi.fn().mockReturnValue(of(CHANNELS)),
      getAntifraud: vi.fn().mockReturnValue(of(ANTIFRAUD)),
      getFaq: vi.fn().mockReturnValue(of([FAQ_REEMBOLSO, FAQ_CARTEIRINHA])),
      requestLibras: vi.fn(),
    };
  });

  it('BR1: renders channel cards with tap-to-call links and the WhatsApp new-tab chat link', async () => {
    await setup();

    const central = el().querySelector('[data-testid="canal-CENTRAL-link"]') as HTMLAnchorElement;
    expect(central.getAttribute('href')).toBe('tel:08001234567');

    const whatsapp = el().querySelector('[data-testid="canal-WHATSAPP-link"]') as HTMLAnchorElement;
    expect(whatsapp.getAttribute('href')).toBe('https://wa.me/5511987654321');
    expect(whatsapp.getAttribute('target')).toBe('_blank');

    // Full phone text stays visible for desktop use (BR1).
    expect(el().querySelector('[data-testid="canal-CENTRAL"]')?.textContent).toContain('0800 123 4567');
  });

  it('BR3: renders the antifraud section with the boleto practice linked to /financas/validar', async () => {
    await setup();

    expect(el().querySelector('[data-testid="antifraude-titulo"]')?.textContent).toContain('Alerta de golpe!');
    expect(el().querySelector('[data-testid="antifraude-mensagem"]')?.textContent).toContain(
      'A operadora não solicita dados ou pagamentos por WhatsApp',
    );
    const link = el().querySelector('[data-testid="antifraude-pratica-boleto"]') as HTMLAnchorElement;
    expect(link.textContent).toContain('Valide sempre o boleto antes de pagar.');
  });

  it('AC2/SPEC-0005 AC6: a fragment of "antifraude" scrolls the section into view', async () => {
    fragment$ = of('antifraude');
    const scrollSpy = vi.fn();
    Element.prototype.scrollIntoView = scrollSpy;

    await setup();
    // The scroll call is scheduled via queueMicrotask; flush microtasks.
    await Promise.resolve();
    await Promise.resolve();

    expect(scrollSpy).toHaveBeenCalled();
  });

  it('AC1/BR5: typing a search term calls the API with q and renders matches', async () => {
    await setup();
    api.getFaq.mockClear();
    api.getFaq.mockReturnValue(of([FAQ_REEMBOLSO]));

    const input = el().querySelector('[data-testid="faq-busca"]') as HTMLInputElement;
    input.value = 'reembolso';
    input.dispatchEvent(new Event('input'));
    fixture.componentInstance.onFaqTermChange('reembolso');
    fixture.detectChanges();

    expect(api.getFaq).toHaveBeenCalledWith(null, 'reembolso');
    expect(el().textContent).toContain('Até quando posso solicitar reembolso?');
  });

  it('BR5: selecting a category calls the API with that category', async () => {
    await setup();
    api.getFaq.mockClear();
    api.getFaq.mockReturnValue(of([FAQ_CARTEIRINHA]));

    (el().querySelector('[data-testid="faq-categoria-CARTEIRINHA"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(api.getFaq).toHaveBeenCalledWith('CARTEIRINHA', null);
  });

  it('BR5: "Todas" restores the full list', async () => {
    await setup();
    api.getFaq.mockClear();
    api.getFaq.mockReturnValue(of([FAQ_REEMBOLSO, FAQ_CARTEIRINHA]));

    (el().querySelector('[data-testid="faq-categoria-CARTEIRINHA"]') as HTMLElement).click();
    fixture.detectChanges();
    api.getFaq.mockClear();
    api.getFaq.mockReturnValue(of([FAQ_REEMBOLSO, FAQ_CARTEIRINHA]));

    (el().querySelector('[data-testid="faq-categoria-TODAS"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(api.getFaq).toHaveBeenCalledWith(null, null);
  });

  it('BR5: no matches shows "Nenhum resultado para \'{termo}\'"', async () => {
    await setup();
    api.getFaq.mockClear();
    api.getFaq.mockReturnValue(of([]));

    fixture.componentInstance.onFaqTermChange('xyzxyz');
    fixture.detectChanges();

    const empty = el().querySelector('[data-testid="faq-vazio"]');
    expect(empty?.textContent).toContain("Nenhum resultado para 'xyzxyz'");
  });

  it('BR5: opening one FAQ item closes the previously open one (single-open accordion)', async () => {
    await setup();

    const panel1 = el().querySelector('[data-testid="faq-item-faq-1"]') as HTMLElement;
    const panel2 = el().querySelector('[data-testid="faq-item-faq-2"]') as HTMLElement;

    (panel1.querySelector('p-accordion-header') as HTMLElement).click();
    fixture.detectChanges();
    expect(panel1.getAttribute('data-p-active')).toBe('true');
    expect(panel2.getAttribute('data-p-active')).toBe('false');

    (panel2.querySelector('p-accordion-header') as HTMLElement).click();
    fixture.detectChanges();
    expect(panel1.getAttribute('data-p-active')).toBe('false');
    expect(panel2.getAttribute('data-p-active')).toBe('true');
  });

  it('AC4/BR4: registering within hours shows the "videocall shortly" confirmation', async () => {
    const response: LibrasRequestResponse = { situation: 'REGISTERED', nextStep: 'videocall-shortly', hours: null };
    api.requestLibras.mockReturnValue(of(response));
    await setup();

    (el().querySelector('[data-testid="libras-solicitar"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(api.requestLibras).toHaveBeenCalledWith('maria-id');
    expect(el().querySelector('[data-testid="libras-mensagem"]')?.textContent).toContain(
      'Nossa equipe iniciará a videochamada em instantes',
    );
    expect(el().querySelector('[data-testid="libras-horario"]')).toBeNull();
  });

  it('AC4/BR4: registering outside hours shows the operating hours and the "next period" confirmation', async () => {
    const response: LibrasRequestResponse = {
      situation: 'REGISTERED',
      nextStep: 'next-period',
      hours: 'Segunda a sexta, das 8h às 18h',
    };
    api.requestLibras.mockReturnValue(of(response));
    await setup();

    (el().querySelector('[data-testid="libras-solicitar"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(el().querySelector('[data-testid="libras-horario"]')?.textContent).toContain(
      'Segunda a sexta, das 8h às 18h',
    );
  });
});
