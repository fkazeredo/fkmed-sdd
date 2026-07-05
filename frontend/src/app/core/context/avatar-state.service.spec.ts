import { TestBed } from '@angular/core/testing';
import { AvatarStateService } from './avatar-state.service';

/** SPEC-0006 BR3: avatar changes propagate everywhere without a new login via a shared override. */
describe('AvatarStateService', () => {
  let service: AvatarStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AvatarStateService);
  });

  it('falls back to the backend avatarUrl when there is no override', () => {
    expect(service.resolve('pedro-id', '/api/beneficiaries/pedro-id/photo')).toBe(
      '/api/beneficiaries/pedro-id/photo',
    );
    expect(service.resolve('pedro-id', null)).toBeNull();
  });

  it('returns the fallback when the beneficiary id is unknown/undefined', () => {
    expect(service.resolve(undefined, '/fallback')).toBe('/fallback');
  });

  it('after an upload, resolves to the cache-busted photo endpoint (avatar updates everywhere)', () => {
    service.onPhotoChanged('pedro-id');
    const resolved = service.resolve('pedro-id', null);
    expect(resolved).toMatch(/^\/api\/beneficiaries\/pedro-id\/photo\?v=\d+$/);
  });

  it('after a removal, resolves to null (placeholder) even if the backend still reports a url', () => {
    service.onPhotoRemoved('pedro-id');
    expect(service.resolve('pedro-id', '/api/beneficiaries/pedro-id/photo')).toBeNull();
  });

  it('keeps overrides independent per beneficiary', () => {
    service.onPhotoRemoved('maria-id');
    service.onPhotoChanged('pedro-id');
    expect(service.resolve('maria-id', '/x')).toBeNull();
    expect(service.resolve('pedro-id', null)).toMatch(/photo\?v=\d+$/);
    expect(service.resolve('joao-id', '/joao')).toBe('/joao');
  });
});
