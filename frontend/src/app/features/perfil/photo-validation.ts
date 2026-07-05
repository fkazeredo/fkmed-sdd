/** SPEC-0006 BR2: photos are JPG/PNG, at most 5 MB, validated by real content — never extension. */
export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

export type PhotoContentType = 'image/jpeg' | 'image/png';

/** The i18n error keys a client pre-check can raise (mirrors the backend 422 codes). */
export type PhotoRejection = 'profile.photo-invalid-content' | 'profile.photo-too-large';

/**
 * Sniffs the real image type from a file's magic bytes (BR2): JPEG starts `FF D8 FF`, PNG starts
 * with the 8-byte signature `89 50 4E 47 0D 0A 1A 0A`. Anything else (e.g. an executable renamed
 * `.png`) returns null and is refused. Never trusts the file extension.
 */
export function sniffImageType(header: Uint8Array): PhotoContentType | null {
  if (header.length >= 3 && header[0] === 0xff && header[1] === 0xd8 && header[2] === 0xff) {
    return 'image/jpeg';
  }
  const png = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (header.length >= 8 && png.every((byte, index) => header[index] === byte)) {
    return 'image/png';
  }
  return null;
}

/**
 * Client pre-check before upload (BR2): refuses an oversized file or one whose real content is not
 * JPG/PNG. Returns the matching i18n error key, or null when the file may proceed to crop+upload.
 */
export function checkPhoto(sizeBytes: number, header: Uint8Array): PhotoRejection | null {
  if (sizeBytes > MAX_PHOTO_BYTES) {
    return 'profile.photo-too-large';
  }
  if (sniffImageType(header) === null) {
    return 'profile.photo-invalid-content';
  }
  return null;
}

/**
 * Geometry of the largest centered square of a `width`×`height` image (BR2 square-crop preview):
 * the source rectangle the canvas draws from. Extracted as a pure function so the crop maths is
 * unit-tested independently of the canvas.
 */
export function centerSquare(
  width: number,
  height: number,
): { sx: number; sy: number; size: number } {
  const size = Math.min(width, height);
  return { sx: Math.floor((width - size) / 2), sy: Math.floor((height - size) / 2), size };
}
