import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { TeleApi } from './tele.api';
import { TelemedicinaHub } from './telemedicina-hub';

describe('TelemedicinaHub (BR1)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TelemedicinaHub],
      providers: [
        provideRouter([]),
        provideI18n(),
        // The embedded instability banner self-loads the notice; default to none.
        { provide: TeleApi, useValue: { getActiveInstabilityNotice: () => of(null) } },
      ],
    }).compileComponents();
  });

  it('renders the three telemedicine entry cards linking to their routes (BR1)', () => {
    const fixture = TestBed.createComponent(TelemedicinaHub);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const pronto = el.querySelector('[data-testid="telemedicina-hub-pronto"]') as HTMLAnchorElement;
    const agendar = el.querySelector('[data-testid="telemedicina-hub-agendar"]') as HTMLAnchorElement;
    const meus = el.querySelector('[data-testid="telemedicina-hub-meus"]') as HTMLAnchorElement;

    expect(pronto.getAttribute('href')).toContain('/telemedicina/triagem');
    expect(agendar.getAttribute('href')).toContain('/telemedicina/agendar');
    expect(meus.getAttribute('href')).toContain('/telemedicina/agendamentos');
    expect(pronto.textContent).toContain('Pronto Atendimento');
    expect(agendar.textContent).toContain('Agendar Consulta');
    expect(meus.textContent).toContain('Meus Agendamentos');
  });
});
