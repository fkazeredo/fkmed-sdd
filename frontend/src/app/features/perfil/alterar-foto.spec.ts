import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AvatarStateService } from '../../core/context/avatar-state.service';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { AlterarFoto } from './alterar-foto';
import { MAX_PHOTO_BYTES } from './photo-validation';

const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46]);
const EXE = new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x03, 0x00, 0x00, 0x00]);

const ACTIVE = { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' as const };

describe('AlterarFoto (SPEC-0006 BR2/BR3)', () => {
  let http: HttpTestingController;
  let avatar: AvatarStateService;

  beforeEach(async () => {
    URL.createObjectURL = vi.fn(() => 'blob:mock');
    URL.revokeObjectURL = vi.fn();
    await TestBed.configureTestingModule({
      imports: [AlterarFoto],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: BeneficiaryContextService, useValue: { active: () => ACTIVE } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    avatar = TestBed.inject(AvatarStateService);
  });

  afterEach(() => http.verify());

  // Creating the component blob-fetches the current photo (effect); flush it as "no photo" (404).
  async function create(): Promise<AlterarFoto> {
    const fixture = TestBed.createComponent(AlterarFoto);
    await fixture.whenStable();
    http
      .expectOne('/api/beneficiaries/pedro-id/photo')
      .flush(null, { status: 404, statusText: 'Not Found' });
    return fixture.componentInstance;
  }

  it('refuses an executable renamed to an image (AC3) — nothing is staged', async () => {
    const component = await create();
    await component.acceptFile(new Blob([EXE]));
    expect(component.errorKey()).toBe('profile.photo-invalid-content');
    expect(component.pendingBlob()).toBeNull();
  });

  it('refuses a file over 5 MB with the too-large key (BR2)', async () => {
    const component = await create();
    const blob = new Blob([JPEG]);
    Object.defineProperty(blob, 'size', { value: MAX_PHOTO_BYTES + 1 });
    await component.acceptFile(blob);
    expect(component.errorKey()).toBe('profile.photo-too-large');
    expect(component.pendingBlob()).toBeNull();
  });

  it('uploads a staged photo and propagates the new avatar everywhere (BR3, AC4)', async () => {
    const component = await create();
    component.stagePhoto(new Blob([JPEG]));

    component.save();
    const request = http.expectOne('/api/beneficiaries/pedro-id/photo');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body instanceof FormData).toBe(true);
    request.flush(null);

    expect(component.success()).toBe('saved');
    expect(component.pendingBlob()).toBeNull();
    // The shared avatar state now serves the just-uploaded bytes (object URL) for this beneficiary.
    expect(avatar.avatarUrl('pedro-id')).toBe('blob:mock');
  });

  it('removes the photo and returns to the placeholder (BR3)', async () => {
    const component = await create();
    avatar.setFromBlob('pedro-id', new Blob([JPEG])); // pretend a photo is currently shown

    component.remove();
    const request = http.expectOne('/api/beneficiaries/pedro-id/photo');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);

    expect(component.success()).toBe('removed');
    expect(avatar.avatarUrl('pedro-id')).toBeNull();
  });

  it('maps a backend 422 invalid-content on upload to the inline error', async () => {
    const component = await create();
    component.stagePhoto(new Blob([JPEG]));
    component.save();
    http
      .expectOne('/api/beneficiaries/pedro-id/photo')
      .flush({ code: 'profile.photo-invalid-content' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.errorKey()).toBe('profile.photo-invalid-content');
    expect(component.success()).toBeNull();
  });
});
