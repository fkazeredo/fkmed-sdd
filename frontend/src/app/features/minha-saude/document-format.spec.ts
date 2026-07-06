import { formatBrDate, sortByIssuedAtDesc, validityStatus } from './document-format';

describe('document-format (SPEC-0011 BR2/BR4/BR5)', () => {
  describe('formatBrDate', () => {
    it('formats an ISO yyyy-MM-dd string as dd/mm/aaaa (TZ-safe, no Date() parsing)', () => {
      expect(formatBrDate('2026-08-03')).toBe('03/08/2026');
    });
  });

  describe('validityStatus', () => {
    it('BR4: a sick note (validUntil null) has no validity badge', () => {
      expect(validityStatus({ validUntil: null, expired: false })).toBe('none');
    });

    it('BR5: an expired document is "expired" regardless of the validUntil date', () => {
      expect(validityStatus({ validUntil: '2026-06-01', expired: true })).toBe('expired');
    });

    it('a non-expired document with a validUntil date is "valid"', () => {
      expect(validityStatus({ validUntil: '2026-08-03', expired: false })).toBe('valid');
    });
  });

  describe('sortByIssuedAtDesc', () => {
    it('BR2: sorts most-recent-first by issuedAt', () => {
      const items = [
        { id: 'a', issuedAt: '2026-06-01' },
        { id: 'b', issuedAt: '2026-07-04' },
        { id: 'c', issuedAt: '2026-06-15' },
      ];
      expect(sortByIssuedAtDesc(items).map((item) => item.id)).toEqual(['b', 'c', 'a']);
    });

    it('does not mutate the input array', () => {
      const items = [{ id: 'a', issuedAt: '2026-06-01' }, { id: 'b', issuedAt: '2026-07-04' }];
      const original = [...items];
      sortByIssuedAtDesc(items);
      expect(items).toEqual(original);
    });
  });
});
