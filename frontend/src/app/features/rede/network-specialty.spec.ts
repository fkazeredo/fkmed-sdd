import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { NetworkApi, RegistryOption } from './network.api';
import { NetworkFunnelState } from './network-funnel-state.service';
import { NetworkSpecialty } from './network-specialty';

// Real backend shape: a raw RegistryOption[].
const SPECIALTIES: RegistryOption[] = [
  { code: 'CARDIOLOGIA', name: 'Cardiologia' },
  { code: 'DERMATOLOGIA', name: 'Dermatologia' },
  { code: 'PEDIATRIA', name: 'Pediatria' },
];

/** SPEC-0008 BR6: specialty picker (≥ 15 seeded, alphabetical + search) — only reached when the
 * chosen service type has a specialty step (the backend `hasSpecialtyStep` flag, BR5). */
describe('NetworkSpecialty', () => {
  let fixture: ComponentFixture<NetworkSpecialty>;
  let api: { getSpecialties: ReturnType<typeof vi.fn> };
  let funnel: NetworkFunnelState;
  let router: Router;

  beforeEach(async () => {
    sessionStorage.clear();
    api = { getSpecialties: vi.fn().mockReturnValue(of(SPECIALTIES)) };
    await TestBed.configureTestingModule({
      imports: [NetworkSpecialty],
      providers: [provideI18n(), { provide: NetworkApi, useValue: api }],
    }).compileComponents();
    funnel = TestBed.inject(NetworkFunnelState);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function setup(): void {
    funnel.setUf('RJ', 'Rio de Janeiro');
    funnel.setMunicipality('Rio de Janeiro');
    funnel.setNeighborhood('Centro');
    funnel.setServiceType('CONSULTORIOS', 'Consultórios–Clínicas–Terapias', true);
    fixture = TestBed.createComponent(NetworkSpecialty);
    fixture.detectChanges();
  }

  it('redirects to /rede/busca/tipo-servico when the service type has no specialty step (defensive)', () => {
    funnel.setUf('RJ', 'Rio de Janeiro');
    funnel.setMunicipality('Rio de Janeiro');
    funnel.setServiceType('LABORATORIOS', 'Laboratórios e Exames', false);
    fixture = TestBed.createComponent(NetworkSpecialty);
    fixture.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/tipo-servico']);
  });

  it('lists every specialty from the registry, alphabetically grouped', () => {
    setup();
    const groups = Array.from(fixture.nativeElement.querySelectorAll('[data-testid^="option-group-"]')).map((el) =>
      (el as HTMLElement).getAttribute('data-testid'),
    );
    expect(groups).toEqual(['option-group-C', 'option-group-D', 'option-group-P']);
  });

  it('filters in real time, accent/case-insensitive (BR6)', () => {
    setup();
    const input = fixture.nativeElement.querySelector('[data-testid="option-search-input"]') as HTMLInputElement;
    input.value = 'CARDIO';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[data-testid^="option-item-"]')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('[data-testid="option-item-CARDIOLOGIA"]')?.textContent).toContain(
      'Cardiologia',
    );
  });

  it('choosing a specialty stores it and navigates to results (AC2)', () => {
    setup();
    (fixture.nativeElement.querySelector('[data-testid="option-item-CARDIOLOGIA"]') as HTMLElement).click();
    expect(funnel.selection().specialty).toBe('CARDIOLOGIA');
    expect(funnel.selection().specialtyName).toBe('Cardiologia');
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/resultados']);
  });

  it('tapping the summary returns to the service-type step, preserving the selection', () => {
    setup();
    (fixture.nativeElement.querySelector('[data-testid="especialidade-resumo"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca/tipo-servico']);
    expect(funnel.selection().serviceType).toBe('CONSULTORIOS');
  });
});
