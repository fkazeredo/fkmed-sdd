import { provideRouter } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { provideI18n } from '../i18n/provide-i18n';
import { AuthService } from '../auth/auth.service';
import { Shell } from './shell';

/** SPEC-0001 §Scope: shell with top bar and navigation placeholder, all strings pt-BR (BR7). */
describe('Shell', () => {
  const auth = {
    username: () => 'maria',
    logout: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [provideRouter([]), provideI18n(), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });

  it('renders the brand, the logged user and the Meu Plano navigation in pt-BR', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="brand"]')?.textContent).toContain('FKMed');
    expect(el.querySelector('[data-testid="username"]')?.textContent).toContain('maria');
    expect(el.querySelector('[data-testid="nav-meu-plano"]')?.textContent).toContain('Meu Plano');
    expect(el.textContent).toContain('Mais funcionalidades em breve');
    expect(el.textContent).toContain('Sair');
  });

  it('logs out through the AuthService', async () => {
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    expect(auth.logout).toHaveBeenCalled();
  });
});
