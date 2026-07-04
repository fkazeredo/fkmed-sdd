/**
 * Client-side mirror of the server password policy base rule (SPEC-0002 BR9/BR16): minimum 8
 * characters, at least 1 letter and 1 digit. The server remains authoritative (BR9 is fully
 * re-validated there, including the common-passwords denylist this mirror cannot see).
 */
export function meetsPasswordPolicy(password: string): boolean {
  return password.length >= 8 && /[a-zA-Z]/.test(password) && /\d/.test(password);
}

/** BR9: the password must not equal the account's login e-mail (case/whitespace insensitive). */
export function differsFromEmail(password: string, email: string): boolean {
  return password.trim().toLowerCase() !== email.trim().toLowerCase();
}
