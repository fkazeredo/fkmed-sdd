package com.fkmed.domain.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * The stored medical-order attachment of an exam appointment (SPEC-0009 BR4): an opaque storage
 * reference and the content type sniffed from the upload ({@link MedicalOrderContent}). One per
 * appointment.
 */
@Entity
@Table(name = "appointment_attachment")
@Getter
public class AppointmentAttachment {

  @Id
  @Column(name = "appointment_id")
  private UUID appointmentId;

  @Column(name = "storage_reference", nullable = false, length = 220)
  private String storageReference;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt;

  /** JPA only. */
  protected AppointmentAttachment() {}

  private AppointmentAttachment(
      UUID appointmentId,
      String storageReference,
      String contentType,
      String fileName,
      Instant uploadedAt) {
    this.appointmentId = appointmentId;
    this.storageReference = storageReference;
    this.contentType = contentType;
    this.fileName = fileName;
    this.uploadedAt = uploadedAt;
  }

  /** Stores the validated medical order for the given appointment. */
  static AppointmentAttachment of(
      UUID appointmentId,
      String storageReference,
      MedicalOrderContent order,
      String fileName,
      Instant uploadedAt) {
    return new AppointmentAttachment(
        appointmentId, storageReference, order.contentType(), fileName, uploadedAt);
  }
}
