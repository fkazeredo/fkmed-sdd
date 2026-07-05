import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { legalAcceptanceGuard } from './legal-acceptance.guard';
import { LegalDocumentsService } from './legal-documents.service';

/** A minimal stand-in for the shared service — only what the guard reads. */
class FakeLegal {
  constructor(
    private readonly pending: boolean,
    private readonly loadFails = false,
  ) {}
  ensureLoaded(): Observable<unknown> {
    return this.loadFails ? throwError(() => new Error('boom')) : of({});
  }
  hasPending(): boolean {
    return this.pending;
  }
}

function run(url: string): boolean | UrlTree {
  const childRoute = {} as ActivatedRouteSnapshot;
  const state = { url } as RouterStateSnapshot;
  let result!: boolean | UrlTree;
  TestBed.runInInjectionContext(() => {
    (legalAcceptanceGuard(childRoute, state) as Observable<boolean | UrlTree>).subscribe(
      (value) => (result = value),
    );
  });
  return result;
}

describe('legalAcceptanceGuard (SPEC-0006 BR8)', () => {
  function configure(legal: FakeLegal): void {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: LegalDocumentsService, useValue: legal }],
    });
  }

  it('redirects any internal route to the acceptance screen while a version is pending (AC5)', () => {
    configure(new FakeLegal(true));
    const result = run('/home');
    const router = TestBed.inject(Router);
    expect(result).toEqual(router.parseUrl('/aceite-legal'));
  });

  it('lets the user stay on the acceptance screen while pending', () => {
    configure(new FakeLegal(true));
    expect(run('/aceite-legal')).toBe(true);
  });

  it('allows normal navigation once nothing is pending', () => {
    configure(new FakeLegal(false));
    expect(run('/meu-plano')).toBe(true);
  });

  it('sends the user home if they open the acceptance screen with nothing pending', () => {
    configure(new FakeLegal(false));
    const router = TestBed.inject(Router);
    expect(run('/aceite-legal')).toEqual(router.parseUrl('/home'));
  });

  it('fails open on a load error — a transient failure must not lock the whole app', () => {
    configure(new FakeLegal(true, true));
    expect(run('/home')).toBe(true);
  });
});
