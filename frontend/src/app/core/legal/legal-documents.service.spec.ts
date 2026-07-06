import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LegalDocumentsCurrent } from './legal.api';
import { LegalDocumentsService } from './legal-documents.service';

const BOTH_ACCEPTED: LegalDocumentsCurrent = {
  terms: { version: '2.0', publishedAt: '2026-06-01', acceptedByMe: true },
  privacy: { version: '1.0', publishedAt: '2026-01-01', acceptedByMe: true },
};

const TERMS_PENDING: LegalDocumentsCurrent = {
  terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false },
  privacy: { version: '1.0', publishedAt: '2026-01-01', acceptedByMe: true },
};

describe('LegalDocumentsService (SPEC-0006 BR8)', () => {
  let service: LegalDocumentsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LegalDocumentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the snapshot once and caches it (no second request)', () => {
    service.ensureLoaded().subscribe();
    http.expectOne('/api/legal-documents/current').flush(BOTH_ACCEPTED);

    service.ensureLoaded().subscribe();
    http.expectNone('/api/legal-documents/current');
    expect(service.hasPending()).toBe(false);
  });

  it('flags a pending mandatory version when a document is unaccepted', () => {
    service.ensureLoaded().subscribe();
    http.expectOne('/api/legal-documents/current').flush(TERMS_PENDING);

    expect(service.hasPending()).toBe(true);
    expect(service.pendingTypes()).toEqual(['TERMS']);
  });

  it('flips to no-pending after markAccepted, so navigation can resume', () => {
    service.ensureLoaded().subscribe();
    http.expectOne('/api/legal-documents/current').flush(TERMS_PENDING);

    service.markAccepted('TERMS');
    expect(service.hasPending()).toBe(false);
    expect(service.current()?.terms.acceptedByMe).toBe(true);
  });

  it('reload() drops the cache and re-fetches (after a 409 a newer version may exist)', () => {
    service.ensureLoaded().subscribe();
    http.expectOne('/api/legal-documents/current').flush(BOTH_ACCEPTED);

    service.reload().subscribe();
    http.expectOne('/api/legal-documents/current').flush(TERMS_PENDING);
    expect(service.pendingTypes()).toEqual(['TERMS']);
  });
});
