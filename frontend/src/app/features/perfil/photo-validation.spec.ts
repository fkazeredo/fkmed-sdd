import { centerSquare, checkPhoto, MAX_PHOTO_BYTES, sniffImageType } from './photo-validation';

const JPEG_HEADER = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]);
const PNG_HEADER = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00]);
// "MZ..." — a Windows executable renamed to .png (SPEC-0006 AC3).
const EXE_HEADER = new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x03, 0x00, 0x00, 0x00]);

describe('photo-validation (SPEC-0006 BR2)', () => {
  describe('sniffImageType — magic bytes, never the extension', () => {
    it('recognises a JPEG by FF D8 FF', () => {
      expect(sniffImageType(JPEG_HEADER)).toBe('image/jpeg');
    });

    it('recognises a PNG by its 8-byte signature', () => {
      expect(sniffImageType(PNG_HEADER)).toBe('image/png');
    });

    it('rejects an executable disguised as an image', () => {
      expect(sniffImageType(EXE_HEADER)).toBeNull();
    });

    it('rejects a PNG signature truncated below 8 bytes', () => {
      expect(sniffImageType(new Uint8Array([0x89, 0x50, 0x4e, 0x47]))).toBeNull();
    });
  });

  describe('checkPhoto', () => {
    it('accepts a valid small JPEG (null = ok)', () => {
      expect(checkPhoto(2048, JPEG_HEADER)).toBeNull();
    });

    it('refuses a file over 5 MB with the too-large key', () => {
      expect(checkPhoto(MAX_PHOTO_BYTES + 1, JPEG_HEADER)).toBe('profile.photo-too-large');
    });

    it('refuses an executable renamed .png with the invalid-content key (AC3)', () => {
      expect(checkPhoto(1024, EXE_HEADER)).toBe('profile.photo-invalid-content');
    });

    it('checks size before content', () => {
      // Oversized AND not an image → the size rejection wins (cheaper, checked first).
      expect(checkPhoto(MAX_PHOTO_BYTES + 1, EXE_HEADER)).toBe('profile.photo-too-large');
    });
  });

  describe('centerSquare', () => {
    it('crops a landscape image to its centered square', () => {
      expect(centerSquare(200, 100)).toEqual({ sx: 50, sy: 0, size: 100 });
    });

    it('crops a portrait image to its centered square', () => {
      expect(centerSquare(100, 200)).toEqual({ sx: 0, sy: 50, size: 100 });
    });

    it('leaves an already-square image unchanged', () => {
      expect(centerSquare(120, 120)).toEqual({ sx: 0, sy: 0, size: 120 });
    });
  });
});
