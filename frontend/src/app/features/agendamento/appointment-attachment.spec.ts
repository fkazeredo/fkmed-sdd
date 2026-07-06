import {
  checkAttachment,
  MAX_ATTACHMENT_BYTES,
  sniffAttachmentType,
} from './appointment-attachment';

const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00]);
const PNG = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
const PDF = new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x37]);
const EXE = new Uint8Array([0x4d, 0x5a, 0x90, 0x00]);

describe('appointment attachment pre-check (BR4)', () => {
  describe('sniffAttachmentType', () => {
    it('detects JPEG by its FF D8 FF magic bytes', () => {
      expect(sniffAttachmentType(JPEG)).toBe('image/jpeg');
    });

    it('detects PNG by its 8-byte signature', () => {
      expect(sniffAttachmentType(PNG)).toBe('image/png');
    });

    it('detects PDF by its %PDF signature', () => {
      expect(sniffAttachmentType(PDF)).toBe('application/pdf');
    });

    it('returns null for content that is none of the three (a renamed executable)', () => {
      expect(sniffAttachmentType(EXE)).toBeNull();
    });
  });

  describe('checkAttachment', () => {
    it('accepts a small PDF (returns null)', () => {
      expect(checkAttachment(1024, PDF)).toBeNull();
    });

    it('accepts a small JPG and PNG', () => {
      expect(checkAttachment(1024, JPEG)).toBeNull();
      expect(checkAttachment(1024, PNG)).toBeNull();
    });

    it('rejects an oversized file with appointment.attachment-invalid', () => {
      expect(checkAttachment(MAX_ATTACHMENT_BYTES + 1, PDF)).toBe('appointment.attachment-invalid');
    });

    it('rejects a wrong-content file with appointment.attachment-invalid', () => {
      expect(checkAttachment(1024, EXE)).toBe('appointment.attachment-invalid');
    });

    it('accepts a file exactly at the 5 MB limit', () => {
      expect(checkAttachment(MAX_ATTACHMENT_BYTES, PDF)).toBeNull();
    });
  });
});
