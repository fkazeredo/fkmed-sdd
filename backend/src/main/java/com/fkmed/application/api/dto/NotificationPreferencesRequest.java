package com.fkmed.application.api.dto;

import com.fkmed.domain.notification.PreferenceUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Preference-update request (SPEC-0004 §API Contracts): the e-mail opt-out to apply per event type.
 * Only shape validation lives here; the mandatory-type rule (BR7) is enforced in the domain.
 *
 * @param preferences the per-type opt-out changes to apply.
 */
public record NotificationPreferencesRequest(
    @NotNull @Valid List<NotificationPreferenceItem> preferences) {

  /**
   * One opt-out change.
   *
   * @param type the event type code.
   * @param emailOptOut whether to opt the e-mail channel out for this type.
   */
  public record NotificationPreferenceItem(@NotBlank String type, boolean emailOptOut) {}

  /** Maps the request to the domain's preference-update commands. */
  public List<PreferenceUpdate> toUpdates() {
    return preferences.stream()
        .map(item -> new PreferenceUpdate(item.type(), item.emailOptOut()))
        .toList();
  }
}
