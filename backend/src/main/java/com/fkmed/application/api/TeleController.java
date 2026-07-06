package com.fkmed.application.api;

import com.fkmed.application.api.dto.EnterTeleSessionRequest;
import com.fkmed.application.api.dto.EnterTeleSessionResponse;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.telemedicine.EnterQueueResult;
import com.fkmed.domain.telemedicine.TeleCatalogView;
import com.fkmed.domain.telemedicine.TeleCurrentSession;
import com.fkmed.domain.telemedicine.TeleService;
import com.fkmed.domain.telemedicine.TeleSessionNotFoundException;
import com.fkmed.domain.telemedicine.TeleSessionView;
import com.fkmed.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Telemedicine Pronto Atendimento endpoints (SPEC-0010): the triage catalog, entering/resuming the
 * queue, the live current-session read (plain JSON or an SSE stream by content negotiation —
 * ADR-0016) and leaving the queue. The caller's beneficiary card and acting account are resolved
 * from the JWT (ADR-0009), never client-supplied; family scope, validation and the state machine
 * are enforced in {@code domain.telemedicine.TeleService}.
 */
@RestController
@RequestMapping("/api/tele")
@RequiredArgsConstructor
public class TeleController {

  private final TeleService tele;
  private final TeleSessionStream stream;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /** The symptom registry + current term for the triage screen (BR2/BR4). */
  @GetMapping("/catalog")
  TeleCatalogView catalog() {
    return tele.catalog();
  }

  /** Enters the queue (201) or resumes the existing session (200) — BR5/BR7. */
  @PostMapping("/sessions")
  ResponseEntity<EnterTeleSessionResponse> enter(
      @Valid @RequestBody EnterTeleSessionRequest request) {
    EnterQueueResult result = tele.enterQueue(request.toCommand(callerCard(), authorAccountId()));
    return ResponseEntity.status(result.resumed() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(EnterTeleSessionResponse.from(result));
  }

  /** The current session as plain JSON for a poll/non-streaming client (BR6). */
  @GetMapping(value = "/sessions/current", produces = MediaType.APPLICATION_JSON_VALUE)
  TeleSessionView current() {
    return tele.currentSessionFor(callerCard())
        .map(TeleCurrentSession::view)
        .orElseThrow(TeleSessionNotFoundException::new);
  }

  /** The current session as a live SSE stream that pushes recomputed state (BR6, ADR-0016). */
  @GetMapping(value = "/sessions/current", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter stream() {
    TeleCurrentSession current =
        tele.currentSessionFor(callerCard()).orElseThrow(TeleSessionNotFoundException::new);
    return stream.open(current.sessionId(), current.view());
  }

  /** Leaves the queue, abandoning the session and releasing the place (BR5). */
  @PostMapping("/sessions/current/leave")
  TeleSessionView leave() {
    return tele.leave(callerCard());
  }

  private String callerCard() {
    return userContext.current().beneficiaryCard().orElse(null);
  }

  private UUID authorAccountId() {
    return accounts
        .findByEmail(userContext.current().username())
        .map(AccountCredentials::accountId)
        .orElse(null);
  }
}
