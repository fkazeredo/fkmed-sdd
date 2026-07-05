package com.fkmed.application.api;

import com.fkmed.application.api.dto.UpdateContactsRequest;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.plan.PhotoContent;
import com.fkmed.domain.plan.ProfileService;
import com.fkmed.domain.plan.ProfileView;
import com.fkmed.infra.security.UserContextProvider;
import com.fkmed.infra.web.HttpRequestMetadata;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Profile endpoints (SPEC-0006): read-only contract data + editable contacts, partial contact
 * updates and the avatar photo. Family scope is enforced server-side in {@code
 * domain.plan.ProfileService} against the caller's beneficiary card claim (SPEC-0003) — the {@code
 * {beneficiaryId}} path is authoritative only after that check. The acting account (for the audit
 * trail) is resolved from the JWT, never client-supplied.
 */
@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileService profileService;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /** Read-only contract data + editable contacts of {@code beneficiaryId} (404 out of scope). */
  @GetMapping("/{beneficiaryId}/profile")
  ProfileView profile(@PathVariable UUID beneficiaryId) {
    return profileService.profileFor(callerCard(), beneficiaryId);
  }

  /** Partial contact update (BR6/BR7); returns the refreshed profile. */
  @PatchMapping("/{beneficiaryId}/contacts")
  ProfileView updateContacts(
      @PathVariable UUID beneficiaryId, @Valid @RequestBody UpdateContactsRequest request) {
    return profileService.updateContacts(
        callerCard(),
        authorAccountId(),
        beneficiaryId,
        request.toDomain(),
        HttpRequestMetadata.current());
  }

  /** Uploads/replaces the avatar photo (BR2/BR3); content validated by magic bytes, ≤ 5 MB. */
  @PutMapping("/{beneficiaryId}/photo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void setPhoto(@PathVariable UUID beneficiaryId, @RequestParam("file") MultipartFile file) {
    profileService.setPhoto(
        callerCard(),
        authorAccountId(),
        beneficiaryId,
        bytesOf(file),
        HttpRequestMetadata.current());
  }

  /** Removes the avatar photo, back to the placeholder (BR3). */
  @DeleteMapping("/{beneficiaryId}/photo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void removePhoto(@PathVariable UUID beneficiaryId) {
    profileService.removePhoto(
        callerCard(), authorAccountId(), beneficiaryId, HttpRequestMetadata.current());
  }

  /** Serves the avatar bytes with the stored content type (BR3); 404 when no photo is set. */
  @GetMapping("/{beneficiaryId}/photo")
  ResponseEntity<byte[]> photo(@PathVariable UUID beneficiaryId) {
    return profileService
        .photoFor(callerCard(), beneficiaryId)
        .map(ProfileController::photoResponse)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static ResponseEntity<byte[]> photoResponse(PhotoContent photo) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(photo.contentType()))
        .body(photo.image());
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

  private static byte[] bytesOf(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("could not read the uploaded photo", e);
    }
  }
}
