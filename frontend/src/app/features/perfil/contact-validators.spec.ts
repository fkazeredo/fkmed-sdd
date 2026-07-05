import {
  isValidCep,
  isValidEmail,
  isValidLandline,
  isValidMobile,
  isValidUf,
} from './contact-validators';

describe('contact-validators (SPEC-0006 validation rules)', () => {
  it('validates the contact e-mail format', () => {
    expect(isValidEmail('maria@fkmed.com')).toBe(true);
    expect(isValidEmail('sem-arroba')).toBe(false);
    expect(isValidEmail('')).toBe(false);
  });

  it('validates the mobile mask (99) 99999-9999', () => {
    expect(isValidMobile('(21) 99999-1234')).toBe(true);
    expect(isValidMobile('21999991234')).toBe(false);
    expect(isValidMobile('(21) 9999-1234')).toBe(false);
  });

  it('validates the optional landline mask (99) 9999-9999', () => {
    expect(isValidLandline('(21) 2222-1234')).toBe(true);
    expect(isValidLandline('(21) 99999-1234')).toBe(false);
  });

  it('validates CEP as 8 digits, with or without the mask', () => {
    expect(isValidCep('22222222')).toBe(true);
    expect(isValidCep('22222-222')).toBe(true);
    expect(isValidCep('2222')).toBe(false);
  });

  it('validates UF as a 2-letter code', () => {
    expect(isValidUf('RJ')).toBe(true);
    expect(isValidUf('rj')).toBe(true);
    expect(isValidUf('RJX')).toBe(false);
  });
});
