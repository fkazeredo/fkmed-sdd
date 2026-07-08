export const MAX_REIMBURSEMENT_FILE_BYTES = 2 * 1024 * 1024;
export const MAX_REIMBURSEMENT_TOTAL_BYTES = 20 * 1024 * 1024;

export type UploadErrorKey =
  | 'reembolso.erro.arquivoGrande'
  | 'reembolso.erro.totalGrande'
  | 'reembolso.erro.conteudoInvalido';

export function detectContentType(bytes: Uint8Array): string | null {
  if (bytes.length >= 4 && bytes[0] === 0x25 && bytes[1] === 0x50 && bytes[2] === 0x44 && bytes[3] === 0x46) {
    return 'application/pdf';
  }
  if (bytes.length >= 4 && bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
    return 'image/jpeg';
  }
  if (
    bytes.length >= 8 &&
    bytes[0] === 0x89 &&
    bytes[1] === 0x50 &&
    bytes[2] === 0x4e &&
    bytes[3] === 0x47 &&
    bytes[4] === 0x0d &&
    bytes[5] === 0x0a &&
    bytes[6] === 0x1a &&
    bytes[7] === 0x0a
  ) {
    return 'image/png';
  }
  return null;
}

export async function validateReimbursementFile(file: File, currentTotal: number): Promise<UploadErrorKey | null> {
  if (file.size > MAX_REIMBURSEMENT_FILE_BYTES) {
    return 'reembolso.erro.arquivoGrande';
  }
  if (currentTotal + file.size > MAX_REIMBURSEMENT_TOTAL_BYTES) {
    return 'reembolso.erro.totalGrande';
  }
  const header = new Uint8Array(await file.slice(0, 8).arrayBuffer());
  return detectContentType(header) ? null : 'reembolso.erro.conteudoInvalido';
}

export function totalBytes(files: { file: File }[]): number {
  return files.reduce((sum, item) => sum + item.file.size, 0);
}

