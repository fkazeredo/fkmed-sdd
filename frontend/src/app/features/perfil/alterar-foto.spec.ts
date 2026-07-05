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

  function create(): AlterarFoto {
    return TestBed.createComponent(AlterarFoto).componentInstance;
  }

  it('refuses an executable renamed to an image (AC3) — nothing is staged', async () => {
    const component = create();
    await component.acceptFile(new Blob([EXE]));
    expect(component.errorKey()).toBe('profile.photo-invalid-content');
    expect(component.pendingBlob()).toBeNull();
  });

  it('refuses a file over 5 MB with the too-large key (BR2)', async () => {
    const component = create();
    const blob = new Blob([JPEG]);
    Object.defineProperty(blob, 'size', { value: MAX_PHOTO_BYTES + 1 });
    await component.acceptFile(blob);
    expect(component.errorKey()).toBe('profile.photo-too-large');
    expect(component.pendingBlob()).toBeNull();
  });

  it('uploads a staged photo and propagates the new avatar everywhere (BR3, AC4)', () => {
    const component = create();
    component.stagePhoto(new Blob([JPEG]));

    component.save();
    const request = http.expectOne('/api/beneficiaries/pedro-id/photo');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body instanceof FormData).toBe(true);
    request.flush(null);

    expect(component.success()).toBe('saved');
    expect(component.pendingBlob()).toBeNull();
    // The shared avatar state now points at the (cache-busted) photo endpoint for this beneficiary.
    expect(avatar.resolve('pedro-id', null)).toMatch(/^\/api\/beneficiaries\/pedro-id\/photo\?v=\d+$/);
  });

  it('removes the photo and returns to the placeholder (BR3)', () => {
    const component = create();
    component.remove();
    const request = http.expectOne('/api/beneficiaries/pedro-id/photo');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);

    expect(component.success()).toBe('removed');
    expect(avatar.resolve('pedro-id', '/api/beneficiaries/pedro-id/photo')).toBeNull();
  });

  it('maps a backend 422 invalid-content on upload to the inline error', () => {
    const component = create();
    component.stagePhoto(new Blob([JPEG]));
    component.save();
    http
      .expectOne('/api/beneficiaries/pedro-id/photo')
      .flush({ code: 'profile.photo-invalid-content' }, { status: 422, statusText: 'Unprocessable' });

    expect(component.errorKey()).toBe('profile.photo-invalid-content');
    expect(component.success()).toBeNull();
  });
});
