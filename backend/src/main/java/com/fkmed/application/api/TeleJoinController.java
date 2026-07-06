package com.fkmed.application.api;

import com.fkmed.domain.telemedicine.TeleService;
import com.fkmed.domain.telemedicine.TeleSessionView;
import com.fkmed.infra.security.UserContextProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Entrar na consulta": joins the room of a scheduled teleconsultation (SPEC-0010 BR14, DL-0018).
 * The endpoint lives on the appointment path but belongs to the telemedicine module, which owns the
 * join window (10 minutes before the slot until its end) and the room it opens. The caller's
 * beneficiary card is resolved from the JWT; the appointment is scope-checked by the appointment
 * module.
 */
@RestController
@RequiredArgsConstructor
public class TeleJoinController {

  private final TeleService tele;
  private final UserContextProvider userContext;

  /** Opens (or resumes) the tele room for a scheduled appointment within the join window. */
  @PostMapping("/api/appointments/{id}/join")
  TeleSessionView join(@PathVariable UUID id) {
    return tele.joinScheduled(userContext.current().beneficiaryCard().orElse(null), id);
  }
}
