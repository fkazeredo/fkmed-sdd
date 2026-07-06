package com.fkmed.domain.appointment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Medical-order attachment persistence (SPEC-0009 BR4). */
interface AppointmentAttachmentRepository extends JpaRepository<AppointmentAttachment, UUID> {}
