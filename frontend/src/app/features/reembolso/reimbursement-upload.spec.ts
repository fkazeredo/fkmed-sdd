import {
  detectContentType,
  MAX_REIMBURSEMENT_FILE_BYTES,
  MAX_REIMBURSEMENT_TOTAL_BYTES,
  validateReimbursementFile,
} from './reimbursement-upload';

function file(bytes: number[], sizePadding = 0): File {
  const payload = new Uint8Array(bytes.length + sizePadding);
  payload.set(bytes);
  return new File([payload], 'documento.bin');
}

describe('reimbursement upload validation', () => {
  it('detects PDF, JPG and PNG by magic bytes', () => {
    expect(detectContentType(new Uint8Array([0x25, 0x50, 0x44, 0x46]))).toBe('application/pdf');
    expect(detectContentType(new Uint8Array([0xff, 0xd8, 0xff, 0xe0]))).toBe('image/jpeg');
    expect(
      detectContentType(new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])),
    ).toBe('image/png');
    expect(detectContentType(new Uint8Array([0x00, 0x01, 0x02]))).toBeNull();
  });

  it('rejects a single file above 2 MB', async () => {
    const result = await validateReimbursementFile(
      file([0x25, 0x50, 0x44, 0x46], MAX_REIMBURSEMENT_FILE_BYTES),
      0,
    );
    expect(result).toBe('reembolso.erro.arquivoGrande');
  });

  it('rejects when the total batch exceeds 20 MB', async () => {
    const result = await validateReimbursementFile(
      file([0x25, 0x50, 0x44, 0x46]),
      MAX_REIMBURSEMENT_TOTAL_BYTES,
    );
    expect(result).toBe('reembolso.erro.totalGrande');
  });

  it('rejects unsupported content even when size is valid', async () => {
    const result = await validateReimbursementFile(file([0x00, 0x01, 0x02]), 0);
    expect(result).toBe('reembolso.erro.conteudoInvalido');
  });
});
