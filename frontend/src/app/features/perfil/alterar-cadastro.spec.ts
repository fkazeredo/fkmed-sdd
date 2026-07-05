import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AlterarCadastro } from './alterar-cadastro';
import { BeneficiaryProfile } from './profile.api';

const ACTIVE = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const };

const PROFILE: BeneficiaryProfile = {
  fullName: 'MARIA CLARA SOUZA LIMA',
  cpf: '***.456.789-**',
  birthDate: '1990-05-10',
  cardNumber: '001234567',
  planName: 'PLANO MÉDICO',
  contactEmail: 'maria@fkmed.com',
  mobile: '(21) 99999-1234',
  landline: '',
  cep: '22222-222',
  street: 'Rua A',
  number: '10',
  complement: '',
  neighborhood: 'Centro',
  city: 'Rio de Janeiro',
  uf: 'RJ',
};

describe('AlterarCadastro (SPEC-0006 BR4/BR5/BR6/BR7)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlterarCadastro],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: BeneficiaryContextService, useValue: { active: signal(ACTIVE) } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function loaded(): Promise<{ fixture: ComponentFixture<AlterarCadastro>; component: AlterarCadastro }> {
    const fixture = TestBed.createComponent(AlterarCadastro);
    await fixture.whenStable();
    http.expectOne('/api/beneficiaries/maria-id/profile').flush(PROFILE);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance };
  }

  it('loads the profile: contract data read-only, contact fields into the editable form', async () => {
    const { fixture, component } = await loaded();
    expect(component.profile()?.fullName).toBe('MARIA CLARA SOUZA LIMA');
    expect(component.form.mobile).toBe('(21) 99999-1234');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="contrato-cpf"]')?.textContent).toContain('***.456.789-**');
    // BR4 hint is present.
    expect(el.querySelector('[data-testid="cadastro-contrato-hint"]')?.textContent?.trim().length).toBeGreaterThan(0);
  });

  it('blocks saving with an invalid mobile format (AC1)', async () => {
    const { component } = await loaded();
    component.form.mobile = '21999991234';
    expect(component.mobileValid).toBe(false);
    expect(component.formValid).toBe(false);
  });

  it('blocks saving when the mandatory mobile is emptied (AC7, BR6)', async () => {
    const { component } = await loaded();
    component.form.mobile = '';
    expect(component.formValid).toBe(false);
    component.submit();
    http.expectNone('/api/beneficiaries/maria-id/contacts');
  });

  it('blocks saving when the mandatory contact e-mail is emptied (BR6)', async () => {
    const { component } = await loaded();
    component.form.contactEmail = '';
    expect(component.formValid).toBe(false);
  });

  it('saves partially — only the changed field goes in the PATCH (BR7, AC2)', async () => {
    const { component } = await loaded();
    component.form.contactEmail = 'nova@fkmed.com';

    component.submit();
    const request = http.expectOne('/api/beneficiaries/maria-id/contacts');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ contactEmail: 'nova@fkmed.com' });
    request.flush(null);

    expect(component.success()).toBe(true);
    // Snapshot advanced: reopening/resaving shows no pending change (AC2 persistence intent).
    expect(component.hasChanges).toBe(false);
  });

  it('does nothing when there is no change (partial-save is a no-op)', async () => {
    const { component } = await loaded();
    expect(component.hasChanges).toBe(false);
    component.submit();
    http.expectNone('/api/beneficiaries/maria-id/contacts');
  });

  it('maps a 422 mobile-required to the mobile field (BR6)', async () => {
    const { component } = await loaded();
    component.form.mobile = '(21) 98888-0000';
    component.submit();
    http
      .expectOne('/api/beneficiaries/maria-id/contacts')
      .flush({ code: 'profile.mobile-required' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.errorKey()).toBe('profile.mobile-required');
    expect(component.errorField()).toBe('mobile');
    expect(component.success()).toBe(false);
  });
});
