import { formatBrDate, GUIDE_STATUS_BADGE, showsAuthPassword } from './guide-format';

describe('guide-format', () => {
  it('formatBrDate() converts a bare date to dd/mm/aaaa', () => {
    expect(formatBrDate('2026-08-03')).toBe('03/08/2026');
  });

  it('formatBrDate() ignores a time component when present (TZ-safe)', () => {
    expect(formatBrDate('2026-08-03T00:00:00Z')).toBe('03/08/2026');
  });

  it('GUIDE_STATUS_BADGE has a distinct class per status (BR2)', () => {
    const classes = Object.values(GUIDE_STATUS_BADGE);
    expect(new Set(classes).size).toBe(classes.length);
  });

  it('showsAuthPassword() is true only for AUTORIZADA/PARCIALMENTE_AUTORIZADA (BR5)', () => {
    expect(showsAuthPassword('AUTORIZADA')).toBe(true);
    expect(showsAuthPassword('PARCIALMENTE_AUTORIZADA')).toBe(true);
    expect(showsAuthPassword('EM_ANALISE')).toBe(false);
    expect(showsAuthPassword('NEGADA')).toBe(false);
    expect(showsAuthPassword('CANCELADA')).toBe(false);
    expect(showsAuthPassword('EXECUTADA')).toBe(false);
  });
});
