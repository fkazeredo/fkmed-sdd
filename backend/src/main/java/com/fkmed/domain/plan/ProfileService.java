package com.fkmed.domain.plan;

import com.fkmed.domain.audit.AuditContext;
import com.fkmed.domain.audit.AuditEntry;
import com.fkmed.domain.audit.AuditEventTypes;
import com.fkmed.domain.audit.AuditRecorder;
import com.fkmed.domain.upload.FileStorage;
import com.fkmed.domain.upload.StorageNamespace;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the profile feature (SPEC-0006): read the profile, edit the beneficiary's
 * contacts (BR5-BR7) and manage the avatar photo (BR2/BR3). Every operation is scoped to the
 * caller's family via {@link BeneficiaryAccess#requireInScope} (SPEC-0003) — a dependent may act
 * only for themselves, a titular for themselves and their dependents. Contact and photo changes are
 * audited (SPEC-0003 BR6); a contact e-mail change publishes {@link ContactDataChanged} so the
 * security notice reaches the old and new addresses (the listener is wired by SPEC-0004).
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

  private final BeneficiaryAccess beneficiaryAccess;
  private final BeneficiaryPhotoRepository photos;
  private final FileStorage fileStorage;
  private final UfValidator ufValidator;
  private final AuditRecorder auditRecorder;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /** The profile of {@code targetBeneficiaryId} within the caller's scope (404 out of scope). */
  @Transactional(readOnly = true)
  public ProfileView profileFor(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary target = beneficiaryAccess.requireInScope(beneficiaryCard, targetBeneficiaryId);
    return ProfileView.from(target, beneficiaryAccess.avatarUrlFor(target.getId()));
  }

  /**
   * Applies a partial contact update and returns the refreshed profile (BR6/BR7). Audited in the
   * same transaction as the change; publishes {@link ContactDataChanged} only when the contact
   * e-mail actually changed.
   */
  @Transactional
  public ProfileView updateContacts(
      String beneficiaryCard,
      UUID authorAccountId,
      UUID targetBeneficiaryId,
      ContactUpdate update,
      AuditContext auditContext) {
    Beneficiary target = beneficiaryAccess.requireInScope(beneficiaryCard, targetBeneficiaryId);
    ContactChange change = target.updateContacts(update, ufValidator);
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.CONTACT_DATA_CHANGED,
            authorAccountId,
            target.getId(),
            Map.of("emailChanged", String.valueOf(change.emailChanged())),
            auditContext));
    if (change.emailChanged()) {
      events.publishEvent(
          new ContactDataChanged(
              target.getId(), change.oldEmail(), change.newEmail(), clock.instant()));
    }
    return ProfileView.from(target, beneficiaryAccess.avatarUrlFor(target.getId()));
  }

  /**
   * Sets (creates or replaces) the beneficiary's photo from the uploaded bytes (BR2/BR3). Validates
   * real content and size in the domain; audited.
   *
   * @throws PhotoInvalidContentException when the content is not JPG/PNG (magic bytes).
   * @throws PhotoTooLargeException when the image exceeds 5 MB.
   */
  @Transactional
  public void setPhoto(
      String beneficiaryCard,
      UUID authorAccountId,
      UUID targetBeneficiaryId,
      byte[] bytes,
      AuditContext auditContext) {
    Beneficiary target = beneficiaryAccess.requireInScope(beneficiaryCard, targetBeneficiaryId);
    UUID beneficiaryId = target.getId();
    BeneficiaryPhoto.validateUpload(bytes);
    String newReference = fileStorage.store(StorageNamespace.PROFILE_PHOTO, bytes);
    BeneficiaryPhoto photo =
        photos
            .findById(beneficiaryId)
            .map(
                existing -> {
                  String oldReference = existing.getStorageReference();
                  existing.replace(bytes, newReference, clock.instant());
                  fileStorage.delete(oldReference);
                  return existing;
                })
            .orElseGet(
                () -> BeneficiaryPhoto.of(beneficiaryId, bytes, newReference, clock.instant()));
    photos.save(photo);
    recordPhotoChange(authorAccountId, beneficiaryId, "uploaded", auditContext);
  }

  /** Removes the beneficiary's photo, back to the placeholder (BR3); audited. No-op when absent. */
  @Transactional
  public void removePhoto(
      String beneficiaryCard,
      UUID authorAccountId,
      UUID targetBeneficiaryId,
      AuditContext auditContext) {
    Beneficiary target = beneficiaryAccess.requireInScope(beneficiaryCard, targetBeneficiaryId);
    UUID beneficiaryId = target.getId();
    photos
        .findById(beneficiaryId)
        .ifPresent(
            photo -> {
              photos.delete(photo);
              fileStorage.delete(photo.getStorageReference());
              recordPhotoChange(authorAccountId, beneficiaryId, "removed", auditContext);
            });
  }

  /**
   * The beneficiary's photo bytes + content type within the caller's scope, or empty when no photo
   * is set (the endpoint answers 404 so the client shows the placeholder). Out-of-scope access
   * throws {@link BeneficiaryNotAccessibleException} (404) before existence is checked.
   */
  @Transactional(readOnly = true)
  public Optional<PhotoContent> photoFor(String beneficiaryCard, UUID targetBeneficiaryId) {
    Beneficiary target = beneficiaryAccess.requireInScope(beneficiaryCard, targetBeneficiaryId);
    return photos
        .findById(target.getId())
        .map(
            photo ->
                new PhotoContent(
                    fileStorage.read(photo.getStorageReference()), photo.getContentType()));
  }

  private void recordPhotoChange(
      UUID authorAccountId, UUID beneficiaryId, String action, AuditContext auditContext) {
    auditRecorder.record(
        new AuditEntry(
            AuditEventTypes.PROFILE_PHOTO_CHANGED,
            authorAccountId,
            beneficiaryId,
            Map.of("action", action),
            auditContext));
  }
}
