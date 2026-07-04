import { isValidCpf } from './cpf';

/** SPEC-0002 §Validation Rules / BR16: the client-side CPF check-digit mirror. */
describe('isValidCpf', () => {
  it('accepts known valid CPFs', () => {
    expect(isValidCpf('52998224725')).toBe(true); // MARIA
    expect(isValidCpf('15350946056')).toBe(true); // PEDRO
    expect(isValidCpf('12345678909')).toBe(true); // common test CPF
  });

  it('rejects a tampered first check digit', () => {
    expect(isValidCpf('52998224715')).toBe(false);
  });

  it('rejects a tampered second check digit', () => {
    expect(isValidCpf('52998224724')).toBe(false);
  });

  it('rejects all-repeated digits', () => {
    expect(isValidCpf('11111111111')).toBe(false);
  });

  it('rejects the wrong length', () => {
    expect(isValidCpf('5299822472')).toBe(false);
    expect(isValidCpf('529982247251')).toBe(false);
  });

  it('rejects non-numeric input', () => {
    expect(isValidCpf('5299822472a')).toBe(false);
  });

  it('rejects an empty string', () => {
    expect(isValidCpf('')).toBe(false);
  });
});
