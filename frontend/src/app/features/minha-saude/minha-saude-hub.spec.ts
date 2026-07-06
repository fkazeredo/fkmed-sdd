import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { MinhaSaudeHub } from './minha-saude-hub';

/** SPEC-0011 BR1: the hub gives access to the 3 categories (all enabled — no "em breve" phasing
 * for this slice, unlike the Rede/Agendamento hubs' disabled placeholders). */
describe('MinhaSaudeHub', () => {
  let fixture: ComponentFixture<MinhaSaudeHub>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MinhaSaudeHub],
      providers: [provideRouter([]), provideI18n()],
    }).compileComponents();
    fixture = TestBed.createComponent(MinhaSaudeHub);
    fixture.detectChanges();
  });

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('BR1: renders the 3 category cards, all enabled', () => {
    for (const key of ['exames', 'encaminhamentos', 'receituarios']) {
      const card = el().querySelector(`[data-testid="minha-saude-hub-${key}"]`);
      expect(card, `${key} card must exist`).not.toBeNull();
      expect((card as HTMLAnchorElement).getAttribute('href')).not.toBeNull();
    }
  });

  it('links each card to its list route', () => {
    expect(el().querySelector('[data-testid="minha-saude-hub-exames"]')?.getAttribute('href')).toBe(
      '/minha-saude/exames',
    );
    expect(
      el().querySelector('[data-testid="minha-saude-hub-encaminhamentos"]')?.getAttribute('href'),
    ).toBe('/minha-saude/encaminhamentos');
    expect(
      el().querySelector('[data-testid="minha-saude-hub-receituarios"]')?.getAttribute('href'),
    ).toBe('/minha-saude/receituarios');
  });
});
