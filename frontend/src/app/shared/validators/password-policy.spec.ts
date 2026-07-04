import { differsFromEmail, meetsPasswordPolicy } from './password-policy';

/**
 * SPEC-0002 BR9/BR16 client-side mirror shared by reset-password and Segurança (first-access's
 * own step-2 password field keeps its inline check — untouched, out of this slice's scope). The
 * server remains authoritative; this only avoids an obviously-doomed round trip.
 */
describe('password-policy', () => {
  describe('meetsPasswordPolicy', () => {
    it('rejects fewer than 8 characters', () => {
      expect(meetsPasswordPolicy('Ab1')).toBe(false);
    });

    it('rejects a password with no letter', () => {
      expect(meetsPasswordPolicy('12345678')).toBe(false);
    });

    it('rejects a password with no digit', () => {
      expect(meetsPasswordPolicy('abcdefgh')).toBe(false);
    });

    it('accepts 8+ chars with a letter and a digit', () => {
      expect(meetsPasswordPolicy('Pedro1234')).toBe(true);
    });
  });

  describe('differsFromEmail', () => {
    it('rejects a password equal to the e-mail (case/whitespace insensitive)', () => {
      expect(differsFromEmail(' Pedro@FKMed.local ', 'pedro@fkmed.local')).toBe(false);
    });

    it('accepts a password different from the e-mail', () => {
      expect(differsFromEmail('Pedro1234', 'pedro@fkmed.local')).toBe(true);
    });
  });
});
