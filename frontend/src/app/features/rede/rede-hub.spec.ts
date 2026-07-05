import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { RedeHub } from './rede-hub';

/**
 * SPEC-0008 §Scope: the Rede hub — 4 cards (Busca de rede, Agendamento, Telemedicina, Minha
 * Saúde). Only "Busca de rede" is built in this phase; the other three render disabled with the
 * "em breve" hint (mirrors Home's phased-delivery pattern, SPEC-0005).
 */
describe('RedeHub', () => {
  let fixture: ComponentFixture<RedeHub>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RedeHub],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        provideRouter([]),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RedeHub);
    fixture.detectChanges();
  });

  it('renders the 4 cards', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="rede-hub-buscaDeRede"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rede-hub-agendamento"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rede-hub-telemedicina"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="rede-hub-minhaSaude"]')).not.toBeNull();
  });

  it('only "Busca de rede" is enabled — the other three show the "em breve" hint and are disabled', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="rede-hub-buscaDeRede"]')?.hasAttribute('disabled')).toBe(false);
    expect(el.querySelector('[data-testid="rede-hub-buscaDeRede-em-breve"]')).toBeNull();

    for (const key of ['agendamento', 'telemedicina', 'minhaSaude']) {
      const card = el.querySelector(`[data-testid="rede-hub-${key}"]`) as HTMLButtonElement;
      expect(card.disabled, `${key} must be disabled`).toBe(true);
      expect(el.querySelector(`[data-testid="rede-hub-${key}-em-breve"]`)).not.toBeNull();
    }
  });

  it('"Busca de rede" links to /rede/busca', () => {
    const link = fixture.nativeElement.querySelector('[data-testid="rede-hub-buscaDeRede"]');
    expect(link?.getAttribute('href')).toBe('/rede/busca');
  });
});
