/**
 * Client-side contact/address validators (SPEC-0006 §Validation Rules). These give immediate
 * inline feedback; the server remains the authority (it re-validates and owns the UF registry
 * check, returning `profile.*` codes the screen maps back to the fields).
 */

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MOBILE = /^\(\d{2}\) \d{5}-\d{4}$/;
const LANDLINE = /^\(\d{2}\) \d{4}-\d{4}$/;
const CEP = /^\d{5}-?\d{3}$/;

export const FIELD_MAX = {
  street: 120,
  number: 10,
  complement: 60,
  neighborhood: 80,
  city: 80,
} as const;

/** Contact e-mail: valid format, mandatory (BR6). */
export function isValidEmail(value: string): boolean {
  return EMAIL.test(value.trim());
}

/** Mobile `(99) 99999-9999`, mandatory (BR6). */
export function isValidMobile(value: string): boolean {
  return MOBILE.test(value.trim());
}

/** Landline `(99) 9999-9999`, optional. */
export function isValidLandline(value: string): boolean {
  return LANDLINE.test(value.trim());
}

/** CEP: 8 digits (accepts the `99999-999` mask). */
export function isValidCep(value: string): boolean {
  return CEP.test(value.trim());
}

/** UF: a 2-letter code (the registry membership is verified server-side, `profile.uf-invalid`). */
export function isValidUf(value: string): boolean {
  return /^[A-Za-z]{2}$/.test(value.trim());
}
