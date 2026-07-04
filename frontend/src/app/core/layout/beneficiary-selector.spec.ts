import { TestBed } from '@angular/core/testing';
import { AccessibleBeneficiary } from '../context/accessible-beneficiaries.api';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { provideI18n } from '../i18n/provide-i18n';
import { BeneficiarySelector } from './beneficiary-selector';

const MARIA: AccessibleBeneficiary = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' };
const PEDRO: AccessibleBeneficiary = { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' };

/** SPEC-0003 BR5: shell-header selector — active-beneficiary label, switching and the
 * single-accessible (dependent's own session) static case. */
describe('BeneficiarySelector', () => {
  async function setup(accessible: AccessibleBeneficiary[], active: AccessibleBeneficiary | null) {
    const context = {
      accessible: () => accessible,
      active: () => active,
      load: vi.fn(),
      setActive: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [BeneficiarySelector],
      providers: [provideI18n(), { provide: BeneficiaryContextService, useValue: context }],
    });
    const fixture = TestBed.createComponent(BeneficiarySelector);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, context };
  }

  it('shows the active beneficiary — first name and role (BR5, e.g. "MARIA · Responsável")', async () => {
    const { fixture } = await setup([MARIA, PEDRO], MARIA);
    const el = fixture.nativeElement as HTMLElement;
    const label = el.querySelector('[data-testid="active-beneficiary-name"]')?.textContent ?? '';
    expect(label).toContain('MARIA');
    expect(label).toContain('Responsável');
  });

  it('switching the selection calls context.setActive with the chosen beneficiary id', async () => {
    const { fixture, context } = await setup([MARIA, PEDRO], MARIA);
    fixture.componentInstance.onChange({ value: PEDRO.beneficiaryId });
    expect(context.setActive).toHaveBeenCalledWith('pedro-id');
  });

  it('a single accessible beneficiary renders statically — no dropdown to switch to', async () => {
    const { fixture } = await setup([PEDRO], PEDRO);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('p-select')).toBeNull();
    const label = el.querySelector('[data-testid="active-beneficiary-name"]')?.textContent ?? '';
    expect(label).toContain('PEDRO');
    expect(label).toContain('Dependente');
  });
});
