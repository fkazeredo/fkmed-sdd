import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccessibleBeneficiary } from '../../core/context/accessible-beneficiaries.api';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { CardResponse } from './card.api';
import { DigitalCard } from './digital-card';

const MARIA_ACCESSIBLE: AccessibleBeneficiary = {
  beneficiaryId: 'maria-id',
  firstName: 'MARIA',
  role: 'TITULAR',
};
const PEDRO_ACCESSIBLE: AccessibleBeneficiary = {
  beneficiaryId: 'pedro-id',
  firstName: 'PEDRO',
  role: 'DEPENDENT',
};
const MARIA_CARD: CardResponse = {
  fullName: 'MARIA CLARA SOUZA LIMA',
  cardNumber: '001234567',
  cns: '700000000000001',
  ansRegistration: '326305',
  coverage: 'ESTADUAL',
  planName: 'ADESÃO PRATA RJ QP COPART TP',
  planCategory: 'PRATA',
  additives: ['Urg/emerg Nacional Hr — Assistência'],
};
const PEDRO_CARD: CardResponse = {
  fullName: 'PEDRO SOUZA LIMA',
  cardNumber: '001234575',
  cns: '700000000000002',
  ansRegistration: '326305',
  coverage: 'ESTADUAL',
  planName: 'ADESÃO PRATA RJ QP COPART TP',
  planCategory: 'PRATA',
  additives: ['Urg/emerg Nacional Hr — Assistência'],
};

/** SPEC-0007: the Digital Card screen — visual card + data sheet for the active beneficiary
 * (BR1/BR2/BR9), "Salvar Carteirinha" PDF download (BR3), reload on beneficiary switch (BR4),
 * "Minhas Carteirinhas" (BR5), "Copiar número" (BR6), unavailable state (BR10). Uses the real
 * BeneficiaryContextService (not a fake), matching the Home spec's approach, so the reactivity
 * to the active-beneficiary signal is exercised end to end. */
describe('DigitalCard', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [DigitalCard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
  });

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  async function setup(): Promise<{ fixture: ComponentFixture<DigitalCard> }> {
    const fixture = TestBed.createComponent(DigitalCard);
    await fixture.whenStable();
    return { fixture };
  }

  /** Loads MARIA as the active (default TITULAR) beneficiary and flushes her card. */
  async function activateMaria(fixture: ComponentFixture<DigitalCard>): Promise<void> {
    context.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE, PEDRO_ACCESSIBLE]);
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/cards/maria-id').flush(MARIA_CARD);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  describe('card + data sheet (BR1/BR2/BR9, AC1)', () => {
    it('shows the loading state before the active beneficiary resolves', async () => {
      const { fixture } = await setup();
      expect(fixture.nativeElement.textContent).toContain('Carregando…');
    });

    it('renders MARIA’s visual card and data sheet from the API', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="card-full-name"]')?.textContent).toContain(
        'MARIA CLARA SOUZA LIMA',
      );
      expect(el.querySelector('[data-testid="card-number"]')?.textContent).toContain('001234567');
      expect(el.querySelector('[data-testid="sheet-cns"]')?.textContent).toContain(
        '700000000000001',
      );
      expect(el.querySelector('[data-testid="sheet-ans"]')?.textContent).toContain('326305');
      expect(el.querySelector('[data-testid="sheet-additive"]')?.textContent).toContain(
        'Urg/emerg Nacional Hr — Assistência',
      );

      // BR2: the card's coverage seal and the data sheet's coverage field render the same label.
      expect(el.querySelector('[data-testid="card-coverage-seal"]')?.textContent).toContain(
        'Estadual',
      );
      expect(el.querySelector('[data-testid="sheet-coverage"]')?.textContent).toContain('Estadual');
    });

    it('reloads card and sheet when the active beneficiary switches to PEDRO (BR4)', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      context.setActive('pedro-id');
      await fixture.whenStable();
      fixture.detectChanges();
      http.expectOne('/api/cards/pedro-id').flush(PEDRO_CARD);
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="card-full-name"]')?.textContent).toContain(
        'PEDRO SOUZA LIMA',
      );
      expect(el.querySelector('[data-testid="card-number"]')?.textContent).toContain('001234575');
    });

    it('shows an error state with retry when the card call fails, and retry refetches', async () => {
      const { fixture } = await setup();
      context.load();
      http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE]);
      await fixture.whenStable();
      fixture.detectChanges();
      http
        .expectOne('/api/cards/maria-id')
        .flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain(
        'Não foi possível carregar os dados. Tente novamente.',
      );
      fixture.nativeElement.querySelector('[data-testid="carteirinha-erro"] button')?.click();
      await fixture.whenStable();
      http.expectOne('/api/cards/maria-id').flush(MARIA_CARD);
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="card-full-name"]')).not.toBeNull();
    });
  });

  describe('unavailable state (BR10, AC6)', () => {
    it('shows the "carteirinha indisponível" message on a 409 card.unavailable, with no card rendered', async () => {
      const { fixture } = await setup();
      context.load();
      http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE]);
      await fixture.whenStable();
      fixture.detectChanges();
      http
        .expectOne('/api/cards/maria-id')
        .flush({ code: 'card.unavailable' }, { status: 409, statusText: 'Conflict' });
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="carteirinha-indisponivel"]')?.textContent).toContain(
        'Carteirinha indisponível. Entre em contato com os canais de atendimento.',
      );
      expect(el.querySelector('[data-testid="card-full-name"]')).toBeNull();
    });

    it('maps a 404 context.beneficiary-not-accessible to the scope-denial message', async () => {
      const { fixture } = await setup();
      context.load();
      http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE]);
      await fixture.whenStable();
      fixture.detectChanges();
      http
        .expectOne('/api/cards/maria-id')
        .flush({ code: 'context.beneficiary-not-accessible' }, { status: 404, statusText: 'Not Found' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Beneficiário não encontrado.');
    });
  });

  describe('copiar número (BR6, AC5)', () => {
    beforeEach(() => {
      Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    });

    it('copies exactly the 9 digits and shows a visual confirmation', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="btn-copiar-numero"]') as HTMLElement).click();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('001234567');
      expect(fixture.nativeElement.querySelector('[data-testid="copy-confirmation"]')).not.toBeNull();
    });

    it('hides the confirmation again after a short delay', async () => {
      // Only fake setTimeout/clearTimeout (matching the Home spec's restricted-fake-timers
      // pattern for setInterval/clearInterval) — faking everything also intercepts the
      // microtask-scheduling machinery `fixture.whenStable()` relies on and hangs the test.
      vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
      const { fixture } = await setup();
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="btn-copiar-numero"]') as HTMLElement).click();
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="copy-confirmation"]')).not.toBeNull();

      vi.advanceTimersByTime(3000);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="copy-confirmation"]')).toBeNull();
    });
  });

  describe('Salvar Carteirinha (BR3, AC2)', () => {
    beforeEach(() => {
      (URL as unknown as { createObjectURL: () => string }).createObjectURL = vi
        .fn()
        .mockReturnValue('blob:mock-url');
      (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = vi.fn();
      // A real anchor.click() would navigate jsdom away from the test page — stub it out.
      vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    });

    it('downloads the PDF by triggering a browser download of the returned blob', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="btn-salvar-carteirinha"]') as HTMLElement).click();
      const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
      http.expectOne('/api/cards/maria-id/pdf').flush(pdfBlob);
      await fixture.whenStable();
      fixture.detectChanges();

      expect(URL.createObjectURL).toHaveBeenCalledWith(pdfBlob);
      expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();
      expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
    });

    it('shows an inline error next to the action when PDF generation fails, without hiding the card', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="btn-salvar-carteirinha"]') as HTMLElement).click();
      // responseType is 'blob' (BR3 download), so an error flush must supply a Blob body too —
      // a plain object fails Angular's automatic body conversion for this response type.
      http.expectOne('/api/cards/maria-id/pdf').flush(new Blob(), { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="pdf-erro"]')?.textContent).toContain(
        'Não foi possível gerar o PDF. Tente novamente.',
      );
      // The card itself must stay visible — a PDF failure is not a card-load failure.
      expect(el.querySelector('[data-testid="card-full-name"]')).not.toBeNull();
    });
  });

  describe('Minhas Carteirinhas (BR5, AC4)', () => {
    it('lists exactly the accessible beneficiaries, tagging the active one', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      const el = fixture.nativeElement as HTMLElement;
      const items = el.querySelectorAll('[data-testid^="minha-carteirinha-"]');
      expect(items).toHaveLength(2);
      expect(el.querySelector('[data-testid="minha-carteirinha-maria-id"]')?.textContent).toContain(
        'MARIA',
      );
      expect(
        el.querySelector('[data-testid="minha-carteirinha-maria-id"] [data-testid="carteirinha-selo-ativa"]'),
      ).not.toBeNull();
      expect(
        el.querySelector('[data-testid="minha-carteirinha-pedro-id"] [data-testid="carteirinha-selo-ativa"]'),
      ).toBeNull();
    });

    it('selecting PEDRO in the list switches the active beneficiary and reloads his card (BR5→BR4)', async () => {
      const { fixture } = await setup();
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="minha-carteirinha-pedro-id"]') as HTMLElement).click();
      await fixture.whenStable();
      fixture.detectChanges();
      http.expectOne('/api/cards/pedro-id').flush(PEDRO_CARD);
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="card-full-name"]')?.textContent).toContain(
        'PEDRO SOUZA LIMA',
      );
      expect(
        el.querySelector('[data-testid="minha-carteirinha-pedro-id"] [data-testid="carteirinha-selo-ativa"]'),
      ).not.toBeNull();
    });

    it('a single accessible beneficiary (a dependent’s own session) still lists exactly himself (AC4)', async () => {
      const { fixture } = await setup();
      context.load();
      http.expectOne('/api/context/accessible-beneficiaries').flush([PEDRO_ACCESSIBLE]);
      await fixture.whenStable();
      fixture.detectChanges();
      http.expectOne('/api/cards/pedro-id').flush(PEDRO_CARD);
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelectorAll('[data-testid^="minha-carteirinha-"]')).toHaveLength(1);
      expect(el.querySelector('[data-testid="minha-carteirinha-pedro-id"]')).not.toBeNull();
    });
  });
});
