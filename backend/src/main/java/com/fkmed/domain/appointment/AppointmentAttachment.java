package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * The stored medical-order attachment of an exam appointment (SPEC-0009 BR4): the content-validated
 * bytes and the content type sniffed from them ({@link MedicalOrderContent}). One per appointment.
 */
@Entity
@Table(name = "appointment_attachment")
@Getter
public class AppointmentAttachment {

  @Id
  @Column(name = "appointment_id")
  private UUID appointmentId;

  @Column(nullable = false)
  private byte[] content;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  /** JPA only. */
  protected AppointmentAttachment() {}

  private AppointmentAttachment(
      UUID appointmentId, byte[] content, String contentType, String fileName, Instant uploadedAt) {
    this.appointmentId = appointmentId;
    this.content = content;
    this.contentType = contentType;
    this.fileName = fileName;
    this.uploadedAt = uploadedAt;
  }

  /** Stores the validated medical order for the given appointment. */
  static AppointmentAttachment of(
      UUID appointmentId, MedicalOrderContent order, String fileName, Instant uploadedAt) {
    return new AppointmentAttachment(
        appointmentId, order.content(), order.contentType(), fileName, uploadedAt);
  }
}
