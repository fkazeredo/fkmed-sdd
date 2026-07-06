package com.fkmed.domain.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * The join-relevant projection of a scheduled appointment, exposed by {@link
 * AppointmentService#teleJoinTarget} so the telemedicine module can open its room (SPEC-0010 BR14,
 * DL-0018) without the {@link Appointment} entity leaving the module. {@code telemedicine} is true
 * only for a Telemedicina-modality booking; {@code active} is the persisted (not derived) status
 * being an open commitment.
 */
public record TeleJoinTarget(
    UUID appointmentId,
    UUID beneficiaryId,
    Instant scheduledAt,
    boolean telemedicine,
    boolean active,
    UUID authorAccountId) {}
