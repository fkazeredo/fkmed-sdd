package com.fkmed.application.api.dto;

import com.fkmed.domain.telemedicine.EnterQueueResult;

/**
 * The queue-entry response (SPEC-0010 POST /api/tele/sessions → {@code {state, position,
 * etaMinutes}}). Whether an existing session was resumed (BR7) is carried only in the HTTP status
 * (200 resume vs 201 new), never in the body.
 */
public record EnterTeleSessionResponse(String state, int position, int etaMinutes) {

  public static EnterTeleSessionResponse from(EnterQueueResult result) {
    return new EnterTeleSessionResponse(result.state(), result.position(), result.etaMinutes());
  }
}
