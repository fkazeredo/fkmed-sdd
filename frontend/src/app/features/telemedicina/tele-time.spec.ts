import { formatElapsed, isJoinWindowOpen, parseLocalDateTime } from './tele-time';

describe('tele-time helpers', () => {
  describe('parseLocalDateTime (TZ-safe)', () => {
    it('parses a local ISO datetime without a UTC shift', () => {
      const date = parseLocalDateTime('2026-07-06T15:00');
      expect(date.getFullYear()).toBe(2026);
      expect(date.getMonth()).toBe(6); // July (0-based)
      expect(date.getDate()).toBe(6);
      expect(date.getHours()).toBe(15);
      expect(date.getMinutes()).toBe(0);
    });
  });

  describe('isJoinWindowOpen (BR14/AC6)', () => {
    const SLOT = '2026-07-06T15:00';

    it('is CLOSED 11 minutes before the slot (AC6: still disabled at 14:49)', () => {
      expect(isJoinWindowOpen(SLOT, new Date(2026, 6, 6, 14, 49))).toBe(false);
    });

    it('OPENS exactly 10 minutes before the slot (AC6: enables at 14:50)', () => {
      expect(isJoinWindowOpen(SLOT, new Date(2026, 6, 6, 14, 50))).toBe(true);
    });

    it('stays open at the slot time', () => {
      expect(isJoinWindowOpen(SLOT, new Date(2026, 6, 6, 15, 0))).toBe(true);
    });

    it('stays open until the slot end (30-min consultation)', () => {
      expect(isJoinWindowOpen(SLOT, new Date(2026, 6, 6, 15, 30))).toBe(true);
    });

    it('is CLOSED after the slot end', () => {
      expect(isJoinWindowOpen(SLOT, new Date(2026, 6, 6, 15, 31))).toBe(false);
    });
  });

  describe('formatElapsed (BR9 running duration)', () => {
    it('formats sub-hour spans as MM:SS', () => {
      expect(formatElapsed('2026-07-06T15:00', new Date(2026, 6, 6, 15, 3, 5))).toBe('03:05');
    });

    it('formats spans over an hour as H:MM:SS', () => {
      expect(formatElapsed('2026-07-06T15:00', new Date(2026, 6, 6, 16, 12, 9))).toBe('1:12:09');
    });

    it('clamps a negative span (clock skew) to 00:00', () => {
      expect(formatElapsed('2026-07-06T15:00', new Date(2026, 6, 6, 14, 59))).toBe('00:00');
    });
  });
});
