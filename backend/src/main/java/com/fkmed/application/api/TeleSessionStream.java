package com.fkmed.application.api;

import com.fkmed.domain.telemedicine.TeleService;
import com.fkmed.domain.telemedicine.TeleSessionClosed;
import com.fkmed.domain.telemedicine.TeleSessionView;
import com.fkmed.domain.telemedicine.TeleTurnReached;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The Server-Sent Events transport for the telemedicine queue (ADR-0016, DL-0022): keeps the open
 * emitters per session and re-emits the recomputed {@link TeleSessionView} on a short fixed cadence
 * plus immediately on the session's own transitions (turn reached, closed). A stream is completed
 * once its session reaches a final state. Emitters are cleaned up on completion/timeout/error and
 * removed from the registry (no leaks). Reconnection resumes because the session itself persists
 * across a disconnect (DL-0017); no broker, no client polling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeleSessionStream {

  /**
   * A long-lived stream; on timeout the client's EventSource reconnects and the session persists.
   */
  private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(15).toMillis();

  private final TeleService tele;

  private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

  /**
   * Opens a stream for {@code sessionId}, sends the initial state immediately and registers
   * cleanup.
   */
  public SseEmitter open(UUID sessionId, TeleSessionView initial) {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    emitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(sessionId, emitter));
    emitter.onTimeout(
        () -> {
          emitter.complete();
          remove(sessionId, emitter);
        });
    emitter.onError(error -> remove(sessionId, emitter));
    send(emitter, initial);
    return emitter;
  }

  /** Periodic re-emit of the recomputed state to every open stream (DL-0022). */
  @Scheduled(fixedDelayString = "${app.tele.sse.cadence-ms:3000}")
  public void broadcast() {
    for (UUID sessionId : Set.copyOf(emitters.keySet())) {
      pushLatest(sessionId);
    }
  }

  /** Immediate re-emit when the turn is reached (AFTER_COMMIT so the pushed state is committed). */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onTurnReached(TeleTurnReached event) {
    pushLatest(event.sessionId());
  }

  /** Immediate re-emit + stream close when the session is closed. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onSessionClosed(TeleSessionClosed event) {
    pushLatest(event.sessionId());
  }

  /** The session ids that currently have an open stream (for tests/observability). */
  public Set<UUID> openSessionIds() {
    return Set.copyOf(emitters.keySet());
  }

  /** Completes and deregisters every open stream for a session (used when it goes final). */
  public void complete(UUID sessionId) {
    List<SseEmitter> streams = emitters.remove(sessionId);
    if (streams == null) {
      return;
    }
    for (SseEmitter emitter : streams) {
      try {
        emitter.complete();
      } catch (RuntimeException ignored) {
        // already completed/aborted by the container — nothing to do.
      }
    }
  }

  private void pushLatest(UUID sessionId) {
    if (!emitters.containsKey(sessionId)) {
      return;
    }
    Optional<TeleSessionView> view = tele.viewOf(sessionId);
    if (view.isPresent()) {
      emit(sessionId, view.get());
    } else {
      complete(sessionId);
    }
  }

  private void emit(UUID sessionId, TeleSessionView view) {
    List<SseEmitter> streams = emitters.get(sessionId);
    if (streams == null) {
      return;
    }
    for (SseEmitter emitter : streams) {
      send(emitter, view);
    }
  }

  private void send(SseEmitter emitter, TeleSessionView view) {
    try {
      emitter.send(SseEmitter.event().name("state").data(view, MediaType.APPLICATION_JSON));
    } catch (IOException | IllegalStateException disconnected) {
      emitter.completeWithError(disconnected);
    }
  }

  private void remove(UUID sessionId, SseEmitter emitter) {
    List<SseEmitter> streams = emitters.get(sessionId);
    if (streams != null) {
      streams.remove(emitter);
      if (streams.isEmpty()) {
        emitters.remove(sessionId);
      }
    }
  }
}
