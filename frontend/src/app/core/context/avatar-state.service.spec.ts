import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AvatarStateService } from './avatar-state.service';

/**
 * SPEC-0006 BR3: the avatar is a Bearer-authenticated blob, fetched via HttpClient (the OIDC
 * interceptor injects the token — `<img src>` cannot), turned into an object URL, and propagated
 * everywhere without a new login. jsdom has no `URL.createObjectURL`, so it is stubbed here.
 */
describe('AvatarStateService', () => {
  let service: AvatarStateService;
  let http: HttpTestingController;

  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:mock');
    URL.revokeObjectURL = vi.fn();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AvatarStateService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('returns null (placeholder) for an unknown or undefined beneficiary', () => {
    expect(service.avatarUrl('pedro-id')).toBeNull();
    expect(service.avatarUrl(undefined)).toBeNull();
  });

  it('blob-fetches the photo via an authenticated HttpClient request and exposes an object URL', () => {
    service.load('pedro-id');
    const request = http.expectOne('/api/beneficiaries/pedro-id/photo');
    expect(request.request.method).toBe('GET');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['x'], { type: 'image/png' }));
    expect(service.avatarUrl('pedro-id')).toBe('blob:mock');
  });

  it('resolves a 404 (no photo) to the placeholder', () => {
    service.load('pedro-id');
    http
      .expectOne('/api/beneficiaries/pedro-id/photo')
      .flush(null, { status: 404, statusText: 'Not Found' });
    expect(service.avatarUrl('pedro-id')).toBeNull();
  });

  it('publishes the just-uploaded bytes with no extra fetch (BR3)', () => {
    service.setFromBlob('pedro-id', new Blob(['x']));
    expect(service.avatarUrl('pedro-id')).toBe('blob:mock');
    http.expectNone('/api/beneficiaries/pedro-id/photo');
  });

  it('revokes the previous object URL when the avatar is replaced (no leak)', () => {
    service.setFromBlob('pedro-id', new Blob(['a']));
    service.setFromBlob('pedro-id', new Blob(['b']));
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock');
  });

  it('remove() forces the placeholder and revokes the object URL', () => {
    service.setFromBlob('pedro-id', new Blob(['a']));
    service.remove('pedro-id');
    expect(service.avatarUrl('pedro-id')).toBeNull();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock');
  });

  it('keeps avatars independent per beneficiary', () => {
    service.setFromBlob('pedro-id', new Blob(['a']));
    service.remove('maria-id');
    expect(service.avatarUrl('pedro-id')).toBe('blob:mock');
    expect(service.avatarUrl('maria-id')).toBeNull();
    expect(service.avatarUrl('joao-id')).toBeNull();
  });
});
