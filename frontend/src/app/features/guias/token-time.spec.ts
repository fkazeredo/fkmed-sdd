import { formatCountdown, isTokenExpired, remainingSeconds } from './token-time';

describe('token-time (SPEC-0012 BR9/BR10)', () => {
  const NOW = new Date('2026-07-06T10:00:00Z');

  it('remainingSeconds() computes the full 10 minutes right after generation', () => {
    expect(remainingSeconds('2026-07-06T10:10:00Z', NOW)).toBe(600);
  });

  it('remainingSeconds() clamps to zero once past expiry (never negative)', () => {
    expect(remainingSeconds('2026-07-06T09:59:00Z', NOW)).toBe(0);
  });

  it('formatCountdown() renders mm:ss, including the initial 10:00', () => {
    expect(formatCountdown(600)).toBe('10:00');
    expect(formatCountdown(59)).toBe('00:59');
    expect(formatCountdown(0)).toBe('00:00');
    expect(formatCountdown(65)).toBe('01:05');
  });

  it('isTokenExpired() is false while time remains and true once the countdown reaches 00:00', () => {
    expect(isTokenExpired('2026-07-06T10:10:00Z', NOW)).toBe(false);
    expect(isTokenExpired('2026-07-06T10:00:00Z', NOW)).toBe(true);
    expect(isTokenExpired('2026-07-06T09:59:59Z', NOW)).toBe(true);
  });
});
