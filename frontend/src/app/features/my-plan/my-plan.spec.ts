import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { MyPlan } from './my-plan';
import { MyPlanResponse } from './plan-api';

/** SPEC-0001 AC3 (frontend half) + UI norm 1 (loading/error/success states). */
describe('MyPlan', () => {
  // The canonical payload from SPEC-0001 §Input/Output Examples.
  const payload: MyPlanResponse = {
    plan: {
      name: 'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
      ansRegistration: '326305',
      coverage: 'ESTADUAL',
      copay: true,
      reimbursement: true,
      additives: ['Urg/emerg Nacional Hr — Assistência'],
    },
    members: [
      { fullName: 'MARIA CLARA SOUZA LIMA', role: 'TITULAR', cardNumber: '001234567' },
      { fullName: 'PEDRO SOUZA LIMA', role: 'DEPENDENT', cardNumber: '001234575' },
    ],
  };

  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyPlan],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows the loading state while the API call is in flight', async () => {
    const fixture = TestBed.createComponent(MyPlan);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Carregando…');
    http.expectOne('/api/plan/my-plan').flush(payload);
  });

  it('renders plan data and family members from the API — nothing hardcoded (BR6)', async () => {
    const fixture = TestBed.createComponent(MyPlan);
    await fixture.whenStable();
    http.expectOne('/api/plan/my-plan').flush(payload);
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(fixture.nativeElement.querySelector('[data-testid="plan-name"]').textContent).toContain(
      'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
    );
    expect(fixture.nativeElement.querySelector('[data-testid="plan-ans"]').textContent).toContain(
      '326305',
    );
    // Coverage is rendered as the pt-BR label, never the raw backend code (AC3).
    expect(
      fixture.nativeElement.querySelector('[data-testid="plan-coverage"]').textContent,
    ).toContain('Estadual');
    expect(text).toContain('Urg/emerg Nacional Hr — Assistência');

    const rows = fixture.nativeElement.querySelectorAll('[data-testid="member-row"]');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('MARIA CLARA SOUZA LIMA');
    expect(rows[0].textContent).toContain('Titular');
    expect(rows[0].textContent).toContain('001234567');
    expect(rows[1].textContent).toContain('PEDRO SOUZA LIMA');
    expect(rows[1].textContent).toContain('Dependente');
    expect(rows[1].textContent).toContain('001234575');
  });

  it('shows the error state with a retry action, and retry refetches', async () => {
    const fixture = TestBed.createComponent(MyPlan);
    await fixture.whenStable();
    http
      .expectOne('/api/plan/my-plan')
      .flush({ code: 'internal.error' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Não foi possível carregar os dados. Tente novamente.',
    );
    const retry = fixture.nativeElement.querySelector('button');
    expect(retry.textContent).toContain('Tentar novamente');

    retry.click();
    await fixture.whenStable();
    http.expectOne('/api/plan/my-plan').flush(payload);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="plan-name"]')).not.toBeNull();
  });
});
