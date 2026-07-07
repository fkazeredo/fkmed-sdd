import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Faq } from './faq';
import { FaqEntry } from './support.api';

const ENTRIES: FaqEntry[] = [
  { id: 'q1', category: 'REEMBOLSO', question: 'Qual é o prazo para pedir reembolso?', answer: 'Até 12 meses.', order: 1 },
  { id: 'q2', category: 'REDE', question: 'Como agendar consulta?', answer: 'Use a busca de rede.', order: 2 },
];

/** SPEC-0014 BR5/AC1/AC3: search + category combo drives the server-side query, an empty result
 * shows the "Nenhum resultado" message, and the accordion opens a single question at a time. */
describe('Faq', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Faq],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads with no filters on init', () => {
    const fixture = TestBed.createComponent(Faq);
    fixture.detectChanges();

    const request = http.expectOne((r) => r.url === '/api/support/faq');
    expect(request.request.params.has('q')).toBe(false);
    expect(request.request.params.has('category')).toBe(false);
    request.flush(ENTRIES);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Qual é o prazo para pedir reembolso?');
  });

  it('re-queries the server with the typed term (AC1) and restores the full list on clear', () => {
    const fixture = TestBed.createComponent(Faq);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq').flush(ENTRIES);
    fixture.detectChanges();

    fixture.componentInstance.onQueryInput('reembolso');
    fixture.detectChanges();
    const searchRequest = http.expectOne((r) => r.url === '/api/support/faq' && r.params.get('q') === 'reembolso');
    searchRequest.flush([ENTRIES[0]]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Como agendar consulta?');

    fixture.componentInstance.onQueryInput('');
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq' && !r.params.has('q')).flush(ENTRIES);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Como agendar consulta?');
  });

  it('filters by category', () => {
    const fixture = TestBed.createComponent(Faq);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq').flush(ENTRIES);
    fixture.detectChanges();

    fixture.componentInstance.selectCategory('REDE');
    fixture.detectChanges();
    http
      .expectOne((r) => r.url === '/api/support/faq' && r.params.get('category') === 'REDE')
      .flush([ENTRIES[1]]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Como agendar consulta?');
    expect(fixture.nativeElement.textContent).not.toContain('Qual é o prazo para pedir reembolso?');
  });

  it('shows the empty-state message with the searched term when nothing matches', () => {
    const fixture = TestBed.createComponent(Faq);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq').flush(ENTRIES);
    fixture.detectChanges();

    fixture.componentInstance.onQueryInput('termo-inexistente');
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq' && r.params.get('q') === 'termo-inexistente').flush([]);
    fixture.detectChanges();

    const empty = fixture.nativeElement.querySelector('[data-testid="faq-vazio"]');
    expect(empty?.textContent).toContain("Nenhum resultado para 'termo-inexistente'");
  });

  it('opening a question closes the previously open one (AC3)', () => {
    const fixture = TestBed.createComponent(Faq);
    fixture.detectChanges();
    http.expectOne((r) => r.url === '/api/support/faq').flush(ENTRIES);
    fixture.detectChanges();

    const headers = Array.from(fixture.nativeElement.querySelectorAll('p-accordion-header')) as HTMLElement[];
    headers[0].click();
    fixture.detectChanges();
    expect(fixture.componentInstance['openId']()).toBe('q1');

    headers[1].click();
    fixture.detectChanges();
    expect(fixture.componentInstance['openId']()).toBe('q2');
  });
});
