package com.fkmed.domain.telemedicine;

import java.util.List;
import java.util.UUID;

/**
 * The triage + term acceptance that opens (or resumes) a Pronto Atendimento session (SPEC-0010 POST
 * /api/tele/sessions). The caller's card and acting account are resolved server-side from the JWT
 * (never client-supplied); the attended {@code beneficiaryId} is scope-checked (BR13, author
 * audited).
 */
public record EnterQueueCommand(
    String callerCard,
    UUID authorAccountId,
    UUID beneficiaryId,
    String complaint,
    List<String> symptoms,
    String otherSymptom,
    String duration,
    String termVersion) {}
