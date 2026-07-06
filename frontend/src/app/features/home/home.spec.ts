import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AccessibleBeneficiary } from '../../core/context/accessible-beneficiaries.api';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummary } from '../../core/context/beneficiary-summary.api';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { Home } from './home';
import { HomeBanner, HomeContentResponse, HomeNotice } from './home.api';

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
const MARIA_SUMMARY: BeneficiarySummary = {
  firstName: 'MARIA',
  fullName: 'MARIA CLARA SOUZA LIMA',
  role: 'TITULAR',
  planName: 'ADESÃO PRATA RJ QP COPART TP',
  cardNumber: '001234567',
  avatarUrl: null,
};
const PEDRO_SUMMARY: BeneficiarySummary = {
  firstName: 'PEDRO',
  fullName: 'PEDRO SOUZA LIMA',
  role: 'DEPENDENT',
  planName: 'ADESÃO PRATA RJ QP COPART TP',
  cardNumber: '001234575',
  avatarUrl: null,
};
const TELEMEDICINA_NOTICE: HomeNotice = {
  title: 'Instabilidade momentânea da Telemedicina',
  severity: 'ALERT',
  body: 'Estamos normalizando o serviço de Telemedicina.',
  order: 1,
};
const LGPD_NOTICE: HomeNotice = {
  title: 'Lei Geral de Proteção de Dados Pessoais',
  severity: 'INFORMATIVE',
  body: 'Saiba como tratamos seus dados pessoais.',
  order: 2,
};
const FRAUD_BANNER: HomeBanner = {
  title: 'Alerta de golpe!',
  text: 'A operadora não solicita dados ou pagamentos por WhatsApp.',
  buttonLabel: 'Saiba mais',
  destination: '/atendimento#antifraude',
  imageUrl: null,
  order: 1,
};
const BOLETO_BANNER: HomeBanner = {
  title: 'Valide seu boleto',
  text: 'Confira a autenticidade do seu boleto antes de pagar.',
  buttonLabel: 'Validar boleto',
  destination: '/boletos/validar',
  imageUrl: null,
  order: 2,
};

/** SPEC-0005: the Home screen — beneficiary card (BR1/BR2), quick-access carousel (BR3/BR4/BR5),
 * banners (BR6) and notices (BR7), with content-unavailability resilience (BR8). Uses the real
 * BeneficiaryContextService (not a fake) so the card's reactivity to the active-beneficiary
 * signal (BR1/AC5) is exercised end to end, matching the i18n-completeness spec's approach. */
describe('Home', () => {
  let http: HttpTestingController;
  let context: BeneficiaryContextService;

  beforeEach(async () => {
    // BeneficiaryContextService restores the active beneficiary from sessionStorage (SPEC-0003
    // BR5) — jsdom's sessionStorage is shared across tests in this file, so it must be cleared
    // or a previous test's setActive() bleeds into the next one's default-selection logic.
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    context = TestBed.inject(BeneficiaryContextService);
  });

  async function setup(): Promise<{ fixture: ComponentFixture<Home> }> {
    const fixture = TestBed.createComponent(Home);
    await fixture.whenStable();
    return { fixture };
  }

  async function flushContent(fixture: ComponentFixture<Home>, content: HomeContentResponse): Promise<void> {
    http.expectOne('/api/content/home').flush(content);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  /** Loads MARIA as the active (default TITULAR) beneficiary and flushes her card summary. */
  async function activateMaria(fixture: ComponentFixture<Home>): Promise<void> {
    context.load();
    http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE, PEDRO_ACCESSIBLE]);
    await fixture.whenStable();
    fixture.detectChanges();
    http.expectOne('/api/context/beneficiaries/maria-id').flush(MARIA_SUMMARY);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
  });

  describe('beneficiary card (BR1/BR2)', () => {
    it('shows the loading state before the active beneficiary resolves', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });

      expect(fixture.nativeElement.textContent).toContain('Carregando…');
    });

    it('shows MARIA’s greeting, plan name and card number once she resolves as active (AC1)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });
      await activateMaria(fixture);

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="card-greeting"]')?.textContent).toContain('Olá, MARIA');
      expect(el.querySelector('[data-testid="card-plan-name"]')?.textContent).toContain(
        'ADESÃO PRATA RJ QP COPART TP',
      );
      expect(el.querySelector('[data-testid="card-number"]')?.textContent).toContain('001234567');
      expect(el.querySelector('[data-testid="card-avatar"]')?.textContent).toContain('M');
    });

    it('updates the card immediately when the active beneficiary switches to PEDRO (BR1/AC5)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });
      await activateMaria(fixture);

      context.setActive('pedro-id');
      await fixture.whenStable();
      fixture.detectChanges();
      http.expectOne('/api/context/beneficiaries/pedro-id').flush(PEDRO_SUMMARY);
      await fixture.whenStable();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="card-greeting"]')?.textContent).toContain('Olá, PEDRO');
      expect(el.querySelector('[data-testid="card-number"]')?.textContent).toContain('001234575');
    });

    it('shows an error state with retry when the summary call fails, and retry refetches', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });
      context.load();
      http.expectOne('/api/context/accessible-beneficiaries').flush([MARIA_ACCESSIBLE]);
      await fixture.whenStable();
      fixture.detectChanges();
      http
        .expectOne('/api/context/beneficiaries/maria-id')
        .flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain(
        'Não foi possível carregar os dados. Tente novamente.',
      );
      fixture.nativeElement.querySelector('button')?.click();
      await fixture.whenStable();
      http.expectOne('/api/context/beneficiaries/maria-id').flush(MARIA_SUMMARY);
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="card-greeting"]')).not.toBeNull();
    });

    it('clicking the avatar opens the "em breve" dialog (Perfil = SPEC-0006)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });
      await activateMaria(fixture);

      (fixture.nativeElement.querySelector('[data-testid="card-avatar"]') as HTMLElement).click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="home-dialog-mensagem"]')?.textContent).toContain(
        'O perfil completo estará disponível em breve.',
      );

      (fixture.nativeElement.querySelector('[data-testid="home-dialog-close"]') as HTMLElement).click();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="home-dialog-mensagem"]')).toBeNull();
    });
  });

  describe('acesso rápido (BR3/BR4/BR5)', () => {
    it('renders the 9 shortcuts in the exact BR3 order', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });

      const buttons = Array.from(
        fixture.nativeElement.querySelectorAll('button[data-testid^="shortcut-"]'),
      ) as HTMLElement[];
      expect(buttons.map((button) => button.getAttribute('data-testid'))).toEqual([
        'shortcut-reconhecimentoFacial',
        'shortcut-guiasTokens',
        'shortcut-redeCredenciada',
        'shortcut-telemedicina',
        'shortcut-agendamento',
        'shortcut-carteirinha',
        'shortcut-minhaSaude',
        'shortcut-canaisAtendimento',
        'shortcut-alteracaoCadastral',
      ]);
    });

    it('only Reconhecimento Facial is enabled — every other shortcut is disabled with the "em breve" hint (BR4)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });

      const el = fixture.nativeElement as HTMLElement;
      const facial = el.querySelector('[data-testid="shortcut-reconhecimentoFacial"]') as HTMLButtonElement;
      expect(facial.disabled).toBe(false);
      expect(el.querySelector('[data-testid="shortcut-reconhecimentoFacial-em-breve"]')).toBeNull();

      const others = [
        'guiasTokens',
        'redeCredenciada',
        'telemedicina',
        'agendamento',
        'carteirinha',
        'minhaSaude',
        'canaisAtendimento',
        'alteracaoCadastral',
      ];
      for (const key of others) {
        const button = el.querySelector(`[data-testid="shortcut-${key}"]`) as HTMLButtonElement;
        expect(button.disabled, `${key} must be disabled`).toBe(true);
        expect(
          el.querySelector(`[data-testid="shortcut-${key}-em-breve"]`),
          `${key} must show the "em breve" hint`,
        ).not.toBeNull();
      }
    });

    it('clicking Reconhecimento Facial opens the mobile-app dialog (BR3)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });

      (fixture.nativeElement.querySelector('[data-testid="shortcut-reconhecimentoFacial"]') as HTMLElement).click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="home-dialog-mensagem"]')?.textContent).toContain(
        'Reconhecimento facial disponível no aplicativo móvel.',
      );
    });

    it('is operable by keyboard arrows (BR5): ArrowRight moves focus to the next page indicator', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [] });

      const indicatorList = fixture.nativeElement.querySelector('.p-carousel-indicator-list') as HTMLElement;
      const indicatorButtons = Array.from(indicatorList.querySelectorAll('button')) as HTMLButtonElement[];
      expect(indicatorButtons.length).toBeGreaterThan(1);

      indicatorButtons[0].focus();
      expect(document.activeElement).toBe(indicatorButtons[0]);

      indicatorList.dispatchEvent(new KeyboardEvent('keydown', { code: 'ArrowRight', bubbles: true }));

      expect(document.activeElement).toBe(indicatorButtons[1]);
    });
  });

  describe('banners (BR6/BR8)', () => {
    it('renders the active banners from the content endpoint', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      const el = fixture.nativeElement as HTMLElement;
      // PrimeNG's circular carousel clones the edge items for seamless wraparound, so DOM order
      // of `[data-testid="banner-item"]` isn't the content order — assert on the section's full
      // text instead of "the first match".
      const bannersSection = el.querySelector('[data-testid="home-banners"]');
      expect(bannersSection).not.toBeNull();
      expect(bannersSection?.textContent).toContain('Alerta de golpe!');
      expect(bannersSection?.textContent).toContain('Valide seu boleto');
    });

    it('SPEC-0014 closes AC6: the fraud banner (destination /atendimento#...) is enabled, no "em breve"; other undelivered destinations stay disabled', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      expect(fixture.componentInstance.isBannerAvailable(FRAUD_BANNER)).toBe(true);
      expect(fixture.componentInstance.isBannerAvailable(BOLETO_BANNER)).toBe(false);

      const el = fixture.nativeElement as HTMLElement;
      const buttons = Array.from(el.querySelectorAll('[data-testid="banner-button"]')) as HTMLButtonElement[];
      expect(buttons.length).toBeGreaterThan(0);
      // At least one enabled button (the fraud banner) and at least one "em breve" hint (boleto).
      expect(buttons.some((button) => !button.disabled)).toBe(true);
      expect(el.querySelectorAll('[data-testid="banner-em-breve"]').length).toBeGreaterThan(0);
    });

    it('SPEC-0014 AC6/AC2: clicking the fraud banner action navigates to its destination', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      const router = TestBed.inject(Router);
      const navigateSpy = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

      fixture.componentInstance.onBannerButtonClick(FRAUD_BANNER);
      expect(navigateSpy).toHaveBeenCalledWith('/atendimento#antifraude');

      navigateSpy.mockClear();
      fixture.componentInstance.onBannerButtonClick(BOLETO_BANNER);
      expect(navigateSpy).not.toHaveBeenCalled();
    });

    it('rotates automatically every 6 seconds (BR6)', async () => {
      vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      expect(fixture.componentInstance.bannerPage()).toBe(0);
      vi.advanceTimersByTime(6000);
      fixture.detectChanges();
      expect(fixture.componentInstance.bannerPage()).toBe(1);
    });

    it('pauses rotation on hover and resumes on mouse leave (BR6)', async () => {
      vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      const section = fixture.nativeElement.querySelector('[data-testid="home-banners"]') as HTMLElement;
      section.dispatchEvent(new MouseEvent('mouseenter'));
      vi.advanceTimersByTime(6000);
      fixture.detectChanges();
      expect(fixture.componentInstance.bannerPage()).toBe(0);

      section.dispatchEvent(new MouseEvent('mouseleave'));
      vi.advanceTimersByTime(6000);
      fixture.detectChanges();
      expect(fixture.componentInstance.bannerPage()).toBe(1);
    });

    it('pauses rotation on focus and resumes on blur (BR6)', async () => {
      vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [FRAUD_BANNER, BOLETO_BANNER], notices: [] });

      const section = fixture.nativeElement.querySelector('[data-testid="home-banners"]') as HTMLElement;
      section.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));
      vi.advanceTimersByTime(6000);
      fixture.detectChanges();
      expect(fixture.componentInstance.bannerPage()).toBe(0);

      section.dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
      vi.advanceTimersByTime(6000);
      fixture.detectChanges();
      expect(fixture.componentInstance.bannerPage()).toBe(1);
    });
  });

  describe('avisos (BR7)', () => {
    it('opens the top-priority notice by default', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [TELEMEDICINA_NOTICE, LGPD_NOTICE] });

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="notice-1"]')?.getAttribute('data-p-active')).toBe('true');
      expect(el.querySelector('[data-testid="notice-2"]')?.getAttribute('data-p-active')).toBe('false');
    });

    it('shows the ALERT tag only for the alert-severity notice (BR7)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [TELEMEDICINA_NOTICE, LGPD_NOTICE] });

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="notice-1"] [data-testid="notice-alert-tag"]')).not.toBeNull();
      expect(el.querySelector('[data-testid="notice-2"] [data-testid="notice-alert-tag"]')).toBeNull();
    });

    it('allows only one notice open at a time — opening LGPD closes Telemedicina (AC4)', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [TELEMEDICINA_NOTICE, LGPD_NOTICE] });

      const lgpdPanel = fixture.nativeElement.querySelector('[data-testid="notice-2"]') as HTMLElement;
      (lgpdPanel.querySelector('p-accordion-header') as HTMLElement).click();
      fixture.detectChanges();

      expect(lgpdPanel.getAttribute('data-p-active')).toBe('true');
      const telemedicinaPanel = fixture.nativeElement.querySelector('[data-testid="notice-1"]') as HTMLElement;
      expect(telemedicinaPanel.getAttribute('data-p-active')).toBe('false');
    });
  });

  describe('BR8 — content unavailability must not break the Home', () => {
    it('hides banners and notices when GET /api/content/home fails, card and shortcuts keep working', async () => {
      const { fixture } = await setup();
      http.expectOne('/api/content/home').flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();
      await activateMaria(fixture);

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="home-banners"]')).toBeNull();
      expect(el.querySelector('[data-testid="home-notices"]')).toBeNull();
      expect(el.querySelector('[data-testid="card-greeting"]')?.textContent).toContain('Olá, MARIA');
      expect(el.querySelectorAll('button[data-testid^="shortcut-"]')).toHaveLength(9);
    });

    it('hides the sections individually when their arrays come back empty', async () => {
      const { fixture } = await setup();
      await flushContent(fixture, { banners: [], notices: [TELEMEDICINA_NOTICE] });

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="home-banners"]')).toBeNull();
      expect(el.querySelector('[data-testid="home-notices"]')).not.toBeNull();
    });
  });
});
