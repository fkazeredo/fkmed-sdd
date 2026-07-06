import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { TeleSession } from './tele.api';
import { extractSseEvents, TeleSessionStreamService, TeleStreamError } from './tele-session-stream.service';

/** Builds a `Response`-like object whose body streams the given UTF-8 chunks, one per `read()`. */
function streamingResponse(chunks: string[], ok = true, status = 200): Response {
  const encoder = new TextEncoder();
  let i = 0;
  const body = {
    getReader() {
      return {
        read() {
          if (i < chunks.length) {
            return Promise.resolve({ done: false, value: encoder.encode(chunks[i++]) });
          }
          return Promise.resolve({ done: true, value: undefined });
        },
      };
    },
  };
  return { ok, status, body } as unknown as Response;
}

describe('extractSseEvents (pure SSE framing)', () => {
  it('extracts complete data: frames and keeps the partial remainder', () => {
    const { events, rest } = extractSseEvents('data: {"a":1}\n\ndata: {"b":2}\n\ndata: {"c":');
    expect(events).toEqual(['{"a":1}', '{"b":2}']);
    expect(rest).toBe('data: {"c":');
  });

  it('normalizes CRLF frame separators and ignores comment/keep-alive lines', () => {
    const { events } = extractSseEvents(': keep-alive\r\n\r\ndata: {"state":"EM_FILA"}\r\n\r\n');
    expect(events).toEqual(['{"state":"EM_FILA"}']);
  });
});

describe('TeleSessionStreamService (BR6/ADR-0016 — live SSE)', () => {
  let service: TeleSessionStreamService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { accessToken: () => 'tok-123' } }],
    });
    service = TestBed.inject(TeleSessionStreamService);
  });

  afterEach(() => vi.restoreAllMocks());

  it('opens the stream with the event-stream Accept and the Bearer header, emitting each pushed session', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      streamingResponse([
        'data: {"state":"EM_FILA","position":4,"etaMinutes":12}\n\n',
        'data: {"state":"EM_FILA","position":2,"etaMinutes":6}\n\n',
      ]),
    );
    vi.stubGlobal('fetch', fetchMock);

    const seen: TeleSession[] = [];
    await new Promise<void>((resolve, reject) => {
      service.connect().subscribe({ next: (s) => seen.push(s), complete: resolve, error: reject });
    });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/tele/sessions/current');
    expect(init.headers['Accept']).toBe('text/event-stream');
    expect(init.headers['Authorization']).toBe('Bearer tok-123');
    // AC1/BR6: the component-facing stream reflects the position decreasing across events.
    expect(seen.map((s) => s.position)).toEqual([4, 2]);
    expect(seen.map((s) => s.etaMinutes)).toEqual([12, 6]);
  });

  it('errors with a TeleStreamError carrying the status on a non-2xx response (404 no session)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamingResponse([], false, 404)));

    const error = await new Promise<unknown>((resolve) => {
      service.connect().subscribe({ error: resolve });
    });
    expect(error).toBeInstanceOf(TeleStreamError);
    expect((error as TeleStreamError).status).toBe(404);
  });

  it('aborts the fetch when the subscription is torn down', async () => {
    const abort = vi.fn();
    // Never-resolving fetch so the subscription is still open when we unsubscribe.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation((_url: string, init: { signal: AbortSignal }) => {
        init.signal.addEventListener('abort', abort);
        return new Promise<Response>(() => undefined);
      }),
    );

    const sub = service.connect().subscribe();
    sub.unsubscribe();
    await Promise.resolve();
    expect(abort).toHaveBeenCalled();
  });
});
