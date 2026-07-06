import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { NetworkApi, ProviderDetail } from './network.api';
import { NetworkProviderDetail } from './network-provider-detail';

const DETAIL: ProviderDetail = {
  id: 'p1',
  name: 'Dr. João Cardiologista',
  serviceType: 'Consultórios–Clínicas–Terapias',
  specialties: ['Cardiologia', 'Clínica Geral'],
  address: {
    cep: '20000-000',
    street: 'Rua das Flores',
    number: '10',
    complement: 'Sala 202',
    neighborhood: 'Centro',
    municipality: 'Rio de Janeiro',
    uf: 'RJ',
  },
  phone: '(21) 99999-0000',
  seals: [{ code: 'QUALI', name: 'Selo Qualidade', description: 'Excelente avaliação dos beneficiários' }],
};

/** SPEC-0008 BR12/BR13: provider detail — full data, clickable phone, seals with description,
 * "Traçar rota" / "Copiar endereço" actions, and the "prestador indisponível" state (BR13). */
describe('NetworkProviderDetail', () => {
  let fixture: ComponentFixture<NetworkProviderDetail>;
  let api: { getProvider: ReturnType<typeof vi.fn> };

  function setup(): void {
    fixture = TestBed.createComponent(NetworkProviderDetail);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    api = { getProvider: vi.fn().mockReturnValue(of(DETAIL)) };
    await TestBed.configureTestingModule({
      imports: [NetworkProviderDetail],
      providers: [
        provideI18n(),
        { provide: NetworkApi, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'p1' }) } } },
      ],
    }).compileComponents();
  });

  it('loads the provider by the route id', () => {
    setup();
    expect(api.getProvider).toHaveBeenCalledWith('p1');
  });

  it('shows name, service type, specialties, full address and the clickable phone (BR12)', () => {
    setup();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="detalhe-nome"]')?.textContent).toContain('Dr. João Cardiologista');
    expect(el.querySelector('[data-testid="detalhe-servico"]')?.textContent).toContain(
      'Consultórios–Clínicas–Terapias',
    );
    expect(el.querySelector('[data-testid="detalhe-especialidades"]')?.textContent).toContain('Cardiologia');
    expect(el.querySelector('[data-testid="detalhe-especialidades"]')?.textContent).toContain('Clínica Geral');
    expect(el.querySelector('[data-testid="detalhe-endereco"]')?.textContent).toContain('Rua das Flores');
    expect(el.querySelector('[data-testid="detalhe-endereco"]')?.textContent).toContain('20000-000');
    const phoneLink = el.querySelector('[data-testid="detalhe-telefone"]') as HTMLAnchorElement;
    expect(phoneLink.getAttribute('href')).toBe('tel:(21) 99999-0000');
  });

  it('shows seal name always, and its description on demand (hover/touch, BR12)', () => {
    setup();
    const el = fixture.nativeElement as HTMLElement;
    const seal = el.querySelector('[data-testid="detalhe-selo-QUALI"]') as HTMLElement;
    expect(seal.textContent).toContain('Selo Qualidade');
    expect(el.querySelector('[data-testid="detalhe-selo-desc-QUALI"]')).toBeNull();

    seal.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="detalhe-selo-desc-QUALI"]')?.textContent).toContain(
      'Excelente avaliação dos beneficiários',
    );
  });

  describe('Traçar rota (BR12, AC6)', () => {
    beforeEach(() => {
      vi.spyOn(window, 'open').mockImplementation(() => null);
    });

    it('opens the maps service with the full address in a new tab', () => {
      setup();
      (fixture.nativeElement.querySelector('[data-testid="detalhe-tracar-rota"]') as HTMLElement).click();

      expect(window.open).toHaveBeenCalledTimes(1);
      const [url, target] = (window.open as ReturnType<typeof vi.fn>).mock.calls[0];
      expect(target).toBe('_blank');
      expect(url).toContain('https://www.google.com/maps/search/');
      expect(decodeURIComponent(url as string)).toContain('Rua das Flores');
      expect(decodeURIComponent(url as string)).toContain('Rio de Janeiro');
    });
  });

  describe('Copiar endereço (BR12)', () => {
    beforeEach(() => {
      Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    });

    it('copies the full formatted address and shows a confirmation', async () => {
      setup();
      (fixture.nativeElement.querySelector('[data-testid="detalhe-copiar-endereco"]') as HTMLElement).click();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        'Rua das Flores, 10 - Sala 202, Centro, Rio de Janeiro - RJ, 20000-000',
      );
      expect(fixture.nativeElement.querySelector('[data-testid="detalhe-endereco-copiado"]')).not.toBeNull();
    });
  });

  describe('unavailable (BR13)', () => {
    it('shows "prestador indisponível" on a 410 network.provider-unavailable, without rendering the detail', () => {
      api.getProvider.mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'network.provider-unavailable' }, status: 410 })),
      );
      setup();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('[data-testid="detalhe-indisponivel"]')).not.toBeNull();
      expect(el.querySelector('[data-testid="detalhe-nome"]')).toBeNull();
    });
  });
});
