/**
 * Brazilian CPF check-digit validation (SPEC-0002 §Validation Rules / BR16 client mirror). Mirrors
 * the backend's mod-11 algorithm (`com.fkmed.domain.identity.CpfCheckDigits`) so the wizard can
 * reject an invalid CPF before submitting — the server remains authoritative (BR1/BR9 posture: the
 * real refusal is always the generic "registration not found").
 */
export function isValidCpf(cpf: string): boolean {
  if (!/^\d{11}$/.test(cpf) || new Set(cpf.split('')).size === 1) {
    return false;
  }
  return checkDigit(cpf, 9) === Number(cpf[9]) && checkDigit(cpf, 10) === Number(cpf[10]);
}

function checkDigit(cpf: string, length: number): number {
  let sum = 0;
  for (let i = 0; i < length; i++) {
    sum += Number(cpf[i]) * (length + 1 - i);
  }
  const remainder = (sum * 10) % 11;
  return remainder === 10 ? 0 : remainder;
}
