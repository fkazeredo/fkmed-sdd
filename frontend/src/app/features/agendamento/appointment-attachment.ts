/** SPEC-0009 BR4: the medical order is JPG/PNG/PDF, at most 5 MB, validated by real content —
 * never by extension. Mirrors SPEC-0006's photo pre-check (DL-0015: same magic-byte approach,
 * plus the PDF signature). */
export const MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024;

export type AttachmentContentType = 'image/jpeg' | 'image/png' | 'application/pdf';

/** The client pre-check surfaces the same i18n key the backend uses for a wrong type OR size
 * (`appointment.attachment-invalid`, 422 — SPEC-0009 §Error Behavior groups both under it). */
export type AttachmentRejection = 'appointment.attachment-invalid';

/**
 * Sniffs the real type from a file's magic bytes (BR4): JPEG starts `FF D8 FF`, PNG starts with the
 * 8-byte signature `89 50 4E 47 0D 0A 1A 0A`, PDF starts `25 50 44 46` (`%PDF`). Anything else
 * (e.g. an executable renamed `.pdf`) returns null and is refused. Never trusts the extension.
 */
export function sniffAttachmentType(header: Uint8Array): AttachmentContentType | null {
  if (header.length >= 3 && header[0] === 0xff && header[1] === 0xd8 && header[2] === 0xff) {
    return 'image/jpeg';
  }
  const png = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (header.length >= 8 && png.every((byte, index) => header[index] === byte)) {
    return 'image/png';
  }
  const pdf = [0x25, 0x50, 0x44, 0x46];
  if (header.length >= 4 && pdf.every((byte, index) => header[index] === byte)) {
    return 'application/pdf';
  }
  return null;
}

/**
 * Client pre-check before upload (BR4): refuses an oversized file or one whose real content is not
 * JPG/PNG/PDF. Returns the i18n error key, or null when the file may proceed. Both failures map to
 * `appointment.attachment-invalid`, matching the backend's single 422 code.
 */
export function checkAttachment(sizeBytes: number, header: Uint8Array): AttachmentRejection | null {
  if (sizeBytes > MAX_ATTACHMENT_BYTES) {
    return 'appointment.attachment-invalid';
  }
  if (sniffAttachmentType(header) === null) {
    return 'appointment.attachment-invalid';
  }
  return null;
}
