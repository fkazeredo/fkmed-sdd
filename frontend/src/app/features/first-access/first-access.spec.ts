import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AuthService } from '../../core/auth/auth.service';
import { FirstAccess } from './first-access';

/** SPEC-0002 first-access wizard (frontend): step progression, neutral errors and show/hide. */
describe('FirstAccess', () => {
  let http: HttpTestingController;
  const login = vi.fn();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FirstAccess],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: AuthService, useValue: { login } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    login.mockClear();
  });

  afterEach(() => http.verify());

  function verifyOk(): FirstAccess {
    const fixture = TestBed.createComponent(FirstAccess);
    const component = fixture.componentInstance;
    component.cpf = '15350946056';
    component.cardNumber = '001234575';
    component.birthDate = '2007-05-20';
    component.submitStep1();
    const request = http.expectOne('/api/auth/first-access/verify');
    expect(request.request.body).toEqual({
      cpf: '15350946056',
      cardNumber: '001234575',
      birthDate: '2007-05-20',
    });
    request.flush({ registrationToken: 'reg-token' });
    return component;
  }

  it('verify success advances to step 2', () => {
    const component = verifyOk();
    expect(component.step()).toBe(2);
    expect(component.errorKey()).toBeNull();
  });

  it('a triple mismatch shows the single generic message (BR1/BR7) and stays on step 1', () => {
    const fixture = TestBed.createComponent(FirstAccess);
    const component = fixture.componentInstance;
    component.cpf = '15350946056';
    component.cardNumber = '001234575';
    component.birthDate = '2000-01-01';
    component.submitStep1();
    http
      .expectOne('/api/auth/first-access/verify')
      .flush({ code: 'auth.registration-not-found' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.errorKey()).toBe('primeiroAcesso.erro.naoEncontrado');
    expect(component.step()).toBe(1);
  });

  it('an existing account (BR2) surfaces a login affordance', () => {
    const fixture = TestBed.createComponent(FirstAccess);
    const component = fixture.componentInstance;
    component.cpf = '52998224725';
    component.cardNumber = '001234567';
    component.birthDate = '1988-03-12';
    component.submitStep1();
    http
      .expectOne('/api/auth/first-access/verify')
      .flush({ code: 'auth.account-already-exists' }, { status: 409, statusText: 'Conflict' });

    expect(component.errorKey()).toBe('primeiroAcesso.erro.jaExiste');
    expect(component.accountExists()).toBe(true);
  });

  it('a dependent under 18 (BR3) gets the titular guidance', () => {
    const fixture = TestBed.createComponent(FirstAccess);
    const component = fixture.componentInstance;
    component.cpf = '12345678909';
    component.cardNumber = '001234580';
    component.birthDate = '2012-01-01';
    component.submitStep1();
    http
      .expectOne('/api/auth/first-access/verify')
      .flush({ code: 'auth.dependent-underage' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.errorKey()).toBe('primeiroAcesso.erro.menorIdade');
  });

  it('complete success advances to step 3 and sends the acceptance flags', () => {
    const component = verifyOk();
    component.email = 'pedro@fkmed.local';
    component.password = 'Pedro1234';
    component.acceptedTerms = true;
    component.acceptedPrivacy = true;
    component.submitStep2();

    const request = http.expectOne('/api/auth/first-access/complete');
    expect(request.request.body).toMatchObject({
      registrationToken: 'reg-token',
      email: 'pedro@fkmed.local',
      acceptedTerms: true,
      acceptedPrivacy: true,
    });
    request.flush(null);
    expect(component.step()).toBe(3);
  });

  it('maps a duplicate e-mail (BR4) to its inline message', () => {
    const component = verifyOk();
    component.email = 'maria@fkmed.local';
    component.password = 'Pedro1234';
    component.acceptedTerms = true;
    component.acceptedPrivacy = true;
    component.submitStep2();
    http
      .expectOne('/api/auth/first-access/complete')
      .flush({ code: 'auth.email-already-used' }, { status: 409, statusText: 'Conflict' });

    expect(component.errorKey()).toBe('primeiroAcesso.erro.emailEmUso');
    expect(component.step()).toBe(2);
  });

  it('validates the password policy client-side (BR16)', () => {
    const fixture = TestBed.createComponent(FirstAccess);
    const component = fixture.componentInstance;
    component.email = 'pedro@fkmed.local';
    component.password = 'short';
    expect(component.passwordValid).toBe(false);
    component.password = 'Pedro1234';
    expect(component.passwordValid).toBe(true);
  });

  it('toggles password visibility (BR16)', () => {
    const component = TestBed.createComponent(FirstAccess).componentInstance;
    expect(component.showPassword()).toBe(false);
    component.togglePassword();
    expect(component.showPassword()).toBe(true);
  });
});
