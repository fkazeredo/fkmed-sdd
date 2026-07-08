import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AtendimentoHub } from './atendimento-hub';
import { AntifraudContent, SupportChannel } from './support.api';

const CHANNELS: SupportChannel[] = [
  { type: 'CENTRAL', label: 'Central de Atendimento 24h', sublabel: 'Capitais', value: '4004-1234', order: 1 },
  {
    type: 'CENTRAL',
    label: 'Central de Atendimento 24h',
    sublabel: 'Demais localidades',
    value: '0800 123 4567',
    order: 2,
  },
  { type: 'WHATSAPP', label: 'WhatsApp oficial', value: '+55 11 91234-5678', order: 3 },
  { type: 'OUVIDORIA', label: 'Ouvidoria', value: '0800 765 4321', hours: 'Seg. a sex., 8h às 18h', order: 4 },
];
const ANTIFRAUD: AntifraudContent = {
  title: 'Alerta de golpe!',
  message: 'A operadora não solicita dados ou pagamentos por WhatsApp.',
};

/** SPEC-0014 BR1/BR3/AC2/AC5: channel cards group same-type rows, tap-to-call/WhatsApp links carry
 * a valid `tel:`/`wa.me` href, and the antifraud section renders at the `#antifraude` anchor. */
describe('AtendimentoHub', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AtendimentoHub],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function setup() {
    const fixture = TestBed.createComponent(AtendimentoHub);
    fixture.detectChanges();
    http.expectOne('/api/support/channels').flush(CHANNELS);
    http.expectOne('/api/support/antifraud').flush(ANTIFRAUD);
    fixture.detectChanges();
    return fixture;
  }

  it('groups the Central 24h rows under one card with both numbers as tel: links', () => {
    const fixture = setup();
    const el = fixture.nativeElement as HTMLElement;

    const card = el.querySelector('[data-testid="canal-central"]') as HTMLElement;
    expect(card).toBeTruthy();
    const links = Array.from(card.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.getAttribute('href'))).toEqual(['tel:40041234', 'tel:08001234567']);
  });

  it('renders the WhatsApp card as a wa.me link opening in a new tab (AC5)', () => {
    const fixture = setup();
    const el = fixture.nativeElement as HTMLElement;

    const link = el.querySelector('[data-testid="canal-whatsapp"] a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('https://wa.me/5511912345678');
    expect(link.getAttribute('target')).toBe('_blank');
  });

  it('renders the antifraud section at the #antifraude anchor with the operator content (BR3/AC2)', () => {
    const fixture = setup();
    const section = fixture.nativeElement.querySelector('#antifraude');
    expect(section).toBeTruthy();
    expect(section?.getAttribute('data-testid')).toBe('antifraude');
    expect(section?.textContent).toContain('Alerta de golpe!');
    expect(section?.textContent).toContain('A operadora não solicita dados ou pagamentos por WhatsApp.');
  });

  it('shows an error with retry when the channels request fails', async () => {
    const fixture = TestBed.createComponent(AtendimentoHub);
    fixture.detectChanges();
    http.expectOne('/api/support/channels').flush({ code: 'x' }, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/support/antifraud').flush(ANTIFRAUD);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar os dados');

    (fixture.nativeElement.querySelector('button') as HTMLElement).click();
    http.expectOne('/api/support/channels').flush(CHANNELS);
    http.expectOne('/api/support/antifraud').flush(ANTIFRAUD);
    fixture.detectChanges();
  });
});
