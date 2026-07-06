import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { TeleApi } from './tele.api';
import { TeleInstabilityBanner } from './tele-instability-banner';

describe('TeleInstabilityBanner (BR1/AC7)', () => {
  let fixture: ComponentFixture<TeleInstabilityBanner>;
  let api: { getActiveInstabilityNotice: ReturnType<typeof vi.fn> };

  function build(): void {
    fixture = TestBed.createComponent(TeleInstabilityBanner);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = { getActiveInstabilityNotice: vi.fn().mockReturnValue(of(null)) };
    TestBed.configureTestingModule({
      imports: [TeleInstabilityBanner],
      providers: [provideI18n(), { provide: TeleApi, useValue: api }],
    });
  });

  it('renders the operator notice content when instability is active', () => {
    api.getActiveInstabilityNotice.mockReturnValue(
      of({ title: 'Instabilidade momentânea da Telemedicina', body: 'Estamos normalizando o serviço.' }),
    );
    build();
    const banner = (fixture.nativeElement as HTMLElement).querySelector(
      '[data-testid="tele-instabilidade-banner"]',
    );
    expect(banner).not.toBeNull();
    expect(banner?.textContent).toContain('Instabilidade momentânea da Telemedicina');
    expect(banner?.textContent).toContain('Estamos normalizando o serviço.');
  });

  it('renders nothing when there is no active instability notice', () => {
    build();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="tele-instabilidade-banner"]'),
    ).toBeNull();
  });

  it('swallows a content-endpoint failure (banner stays hidden, never breaks the page)', () => {
    api.getActiveInstabilityNotice.mockReturnValue(throwError(() => new Error('down')));
    build();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('[data-testid="tele-instabilidade-banner"]'),
    ).toBeNull();
  });
});
