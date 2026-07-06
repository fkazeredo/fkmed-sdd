import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AgendamentoHub } from './agendamento-hub';

describe('AgendamentoHub', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgendamentoHub],
      providers: [provideRouter([]), provideI18n()],
    }).compileComponents();
  });

  it('renders the three enabled cards linking to their routes', () => {
    const fixture = TestBed.createComponent(AgendamentoHub);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const consulta = el.querySelector('[data-testid="agendamento-hub-consulta"]') as HTMLAnchorElement;
    const exame = el.querySelector('[data-testid="agendamento-hub-exame"]') as HTMLAnchorElement;
    const meus = el.querySelector('[data-testid="agendamento-hub-meus"]') as HTMLAnchorElement;

    expect(consulta.getAttribute('href')).toContain('/agendamento/consulta');
    expect(exame.getAttribute('href')).toContain('/agendamento/exame');
    expect(meus.getAttribute('href')).toContain('/agendamento/meus');
    expect(consulta.textContent).toContain('Agendar Consulta');
    expect(exame.textContent).toContain('Agendar Exame');
    expect(meus.textContent).toContain('Meus Agendamentos');
  });

  it('renders Telemedicina disabled with the "em breve" hint', () => {
    const fixture = TestBed.createComponent(AgendamentoHub);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    const tele = el.querySelector('[data-testid="agendamento-hub-telemedicina"]') as HTMLButtonElement;
    expect(tele.tagName).toBe('BUTTON');
    expect(tele.disabled).toBe(true);
    expect(
      el.querySelector('[data-testid="agendamento-hub-telemedicina-em-breve"]')?.textContent,
    ).toContain('Em breve');
  });
});
