import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentsApi, AvailabilityDay, CareUnit, RegistryOption } from './appointments.api';
import { ExameWizard } from './exame-wizard';

const EXAMS: RegistryOption[] = [
  { code: 'HEMOGRAMA', name: 'Hemograma' },
  { code: 'RAIO_X', name: 'Raio-X' },
];
const UNITS: CareUnit[] = [{ id: 'u1', name: 'Unidade Centro', address: 'Rua A, 10 — Centro' }];
const DAYS: AvailabilityDay[] = [
  { date: '2026-07-10', slots: [{ slot: '2026-07-10T09:00', remaining: 2, available: true }] },
];

/** A real PDF blob (magic bytes %PDF) so the content pre-check accepts it. */
function pdfFile(name = 'pedido.pdf'): File {
  return new File([new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x37])], name, {
    type: 'application/pdf',
  });
}
/** A renamed executable — wrong content, must be refused. */
function fakeFile(): File {
  return new File([new Uint8Array([0x4d, 0x5a, 0x90, 0x00])], 'virus.pdf', { type: 'application/pdf' });
}

describe('ExameWizard (BR4/BR2)', () => {
  let fixture: ComponentFixture<ExameWizard>;
  let api: {
    getExams: ReturnType<typeof vi.fn>;
    getUnits: ReturnType<typeof vi.fn>;
    getAvailability: ReturnType<typeof vi.fn>;
    bookExam: ReturnType<typeof vi.fn>;
  };

  const context = {
    active: () => ({ beneficiaryId: 'b1', firstName: 'MARIA', role: 'TITULAR' as const }),
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function click(testid: string): void {
    (el().querySelector(`[data-testid="${testid}"]`) as HTMLElement).click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    api = {
      getExams: vi.fn().mockReturnValue(of(EXAMS)),
      getUnits: vi.fn().mockReturnValue(of(UNITS)),
      getAvailability: vi.fn().mockReturnValue(of(DAYS)),
      bookExam: vi.fn().mockReturnValue(of({ protocol: 'AG-20260704-0002', status: 'AGENDADO' })),
    };
    await TestBed.configureTestingModule({
      imports: [ExameWizard],
      providers: [
        provideI18n(),
        { provide: AppointmentsApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ExameWizard);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('loads the exam catalog on init (BR4)', () => {
    expect(api.getExams).toHaveBeenCalled();
  });

  it('AC2/BR4: blocks advancing to the unit step without an attachment, showing a message', async () => {
    fixture.componentInstance.selectExam('HEMOGRAMA');
    fixture.detectChanges();
    click('exame-proximo'); // -> attachment step
    expect(fixture.componentInstance['step']()).toBe('attachment');
    click('exame-proximo'); // try to advance with no file
    expect(el().querySelector('[data-testid="exame-gate"]')).not.toBeNull();
    expect(fixture.componentInstance['step']()).toBe('attachment');
    expect(api.getUnits).not.toHaveBeenCalled();
  });

  it('previews the attached file name and lets it be removed (BR4)', async () => {
    fixture.componentInstance.selectExam('HEMOGRAMA');
    fixture.detectChanges();
    click('exame-proximo');
    await fixture.componentInstance.acceptFile(pdfFile('pedido-medico.pdf'));
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="exame-anexo-nome"]')?.textContent).toContain('pedido-medico.pdf');

    click('exame-anexo-remover');
    expect(fixture.componentInstance['attachment']()).toBeNull();
    expect(el().querySelector('[data-testid="exame-anexo-nome"]')).toBeNull();
  });

  it('BR4: refuses a wrong-content file (renamed executable) with an inline message and no staged file', async () => {
    fixture.componentInstance.selectExam('HEMOGRAMA');
    fixture.detectChanges();
    click('exame-proximo');
    await fixture.componentInstance.acceptFile(fakeFile());
    fixture.detectChanges();
    expect(fixture.componentInstance['attachment']()).toBeNull();
    expect(el().querySelector('[data-testid="exame-anexo-erro"]')).not.toBeNull();
  });

  it('completes the full exam flow and confirms multipart with the active beneficiary (BR1/BR7)', async () => {
    fixture.componentInstance.selectExam('HEMOGRAMA');
    fixture.detectChanges();
    click('exame-proximo'); // -> attachment
    await fixture.componentInstance.acceptFile(pdfFile());
    fixture.detectChanges();
    click('exame-proximo'); // -> unit
    expect(api.getUnits).toHaveBeenCalledWith({ exam: 'HEMOGRAMA' });
    fixture.componentInstance.selectUnit('u1');
    fixture.detectChanges();
    click('exame-proximo'); // -> slot
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('exame-proximo'); // -> review

    expect(el().querySelector('[data-testid="revisao-anexo"]')?.textContent).toContain('pedido.pdf');
    click('exame-confirmar');

    expect(api.bookExam).toHaveBeenCalledWith(
      expect.objectContaining({ beneficiaryId: 'b1', exam: 'HEMOGRAMA', unitId: 'u1', slot: '2026-07-10T09:00' }),
    );
    expect(el().querySelector('[data-testid="exame-protocolo"]')?.textContent).toContain('AG-20260704-0002');
  });
});
