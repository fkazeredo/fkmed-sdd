import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { TeleApi, TeleSession } from './tele.api';
import { TeleSessionStreamService, TeleStreamError } from './tele-session-stream.service';
import { SessaoAtendimento } from './sessao-atendimento';

describe('SessaoAtendimento (BR5/BR6/BR8/BR9 — state-driven, ADR-0015)', () => {
  let fixture: ComponentFixture<SessaoAtendimento>;
  let stream$: Subject<TeleSession>;
  let api: { leaveSession: ReturnType<typeof vi.fn>; getActiveInstabilityNotice: ReturnType<typeof vi.fn> };
  let router: Router;

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function emit(session: TeleSession): void {
    stream$.next(session);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    stream$ = new Subject<TeleSession>();
    const streamService = { connect: () => stream$.asObservable() };
    api = {
      leaveSession: vi.fn().mockReturnValue(of(undefined)),
      getActiveInstabilityNotice: vi.fn().mockReturnValue(of(null)),
    };
    await TestBed.configureTestingModule({
      imports: [SessaoAtendimento],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: TeleSessionStreamService, useValue: streamService },
        { provide: TeleApi, useValue: api },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(SessaoAtendimento);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('AC1/BR6: reflects the live position/ETA decreasing as the SSE stream pushes updates', () => {
    emit({ state: 'EM_FILA', position: 4, etaMinutes: 12 });
    expect(el().querySelector('[data-testid="fila-posicao"]')?.textContent).toContain('4');
    expect(el().querySelector('[data-testid="fila-eta"]')?.textContent).toContain('12');

    // A later streamed event moves the queue forward — no user action, no reload.
    emit({ state: 'EM_FILA', position: 2, etaMinutes: 6 });
    expect(el().querySelector('[data-testid="fila-posicao"]')?.textContent).toContain('2');
    expect(el().querySelector('[data-testid="fila-eta"]')?.textContent).toContain('6');
  });

  it('BR8/BR9: when the turn is reached the room opens with the professional and CRM highlighted', () => {
    emit({ state: 'EM_FILA', position: 1, etaMinutes: 2 });
    emit({
      state: 'EM_ATENDIMENTO',
      professional: { name: 'Dra. Ana Souza', crm: 'CRM-RJ 123456' },
      room: { startedAt: '2026-07-06T15:00' },
    });
    expect(el().querySelector('[data-testid="sala-sua-vez"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="sala-profissional"]')?.textContent).toContain('Dra. Ana Souza');
    expect(el().querySelector('[data-testid="sala-crm"]')?.textContent).toContain('CRM-RJ 123456');
    expect(el().querySelector('[data-testid="sala-duracao"]')).not.toBeNull();
  });

  it('BR9/AC4: on ENCERRADA it shows the summary with issued documents and the Minha Saúde link', () => {
    emit({
      state: 'ENCERRADA',
      professional: { name: 'Dra. Ana', crm: 'CRM-RJ 123456' },
      room: {
        durationMinutes: 18,
        guidance: 'Repouso e hidratação.',
        documents: [{ id: 'doc-1', type: 'PRESCRIPTION', description: 'Receita — Dipirona' }],
      },
    });
    expect(el().querySelector('[data-testid="sessao-resumo"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="resumo-orientacoes"]')?.textContent).toContain('Repouso');
    expect(el().querySelector('[data-testid="resumo-documento-doc-1"]')?.textContent).toContain('Receita — Dipirona');
    const link = el().querySelector('[data-testid="resumo-minha-saude"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toContain('/minha-saude');
  });

  it('AC2: confirming "Sair da fila" leaves the session and reopens the hub', () => {
    emit({ state: 'EM_FILA', position: 3, etaMinutes: 8 });
    (el().querySelector('[data-testid="fila-sair"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="sessao-confirmar-saida"]')).not.toBeNull();
    (el().querySelector('[data-testid="sessao-saida-confirmar"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(api.leaveSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/telemedicina']);
  });

  it('AC3: an ABANDONADA (no-show / left) session shows the notice and reopens the hub', () => {
    emit({ state: 'ABANDONADA' });
    expect(el().querySelector('[data-testid="sessao-abandonada"]')).not.toBeNull();
  });

  it('shows the empty state when there is no active session (404 tele.session-not-found)', () => {
    stream$.error(new TeleStreamError(404));
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="sessao-sem-sessao"]')).not.toBeNull();
  });

  it('surfaces a leave error inline without navigating away', () => {
    api.leaveSession.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'x' }, status: 500 })),
    );
    emit({ state: 'EM_FILA', position: 3, etaMinutes: 8 });
    (el().querySelector('[data-testid="fila-sair"]') as HTMLElement).click();
    fixture.detectChanges();
    (el().querySelector('[data-testid="sessao-saida-confirmar"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="sessao-saida-erro"]')).not.toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
