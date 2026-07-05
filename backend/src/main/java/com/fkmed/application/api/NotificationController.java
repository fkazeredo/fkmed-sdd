package com.fkmed.application.api;

import com.fkmed.application.api.dto.NotificationPreferencesRequest;
import com.fkmed.domain.identity.AccountCredentials;
import com.fkmed.domain.identity.IdentityAccounts;
import com.fkmed.domain.notification.NotificationListView;
import com.fkmed.domain.notification.NotificationPreferencesView;
import com.fkmed.domain.notification.Notifications;
import com.fkmed.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification center endpoints (SPEC-0004 §API Contracts). The caller's account comes from the
 * resource-server JWT via {@link UserContextProvider} resolved to an account id ({@link
 * IdentityAccounts}) — never a client-supplied id; every read/write is scoped to it.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private static final int MAX_PAGE_SIZE = 100;

  private final Notifications notifications;
  private final UserContextProvider userContext;
  private final IdentityAccounts accounts;

  /** Paginated list (newest first) plus the unread count for the current account (BR2/BR3). */
  @GetMapping
  NotificationListView list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return notifications.list(accountId(), Math.max(0, page), clampSize(size));
  }

  /** Marks one notification read; unknown/foreign id → 404 (BR2). */
  @PostMapping("/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void markRead(@PathVariable UUID id) {
    notifications.markRead(accountId(), id);
  }

  /** Marks every unread notification of the current account read (BR2). */
  @PostMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void markAllRead() {
    notifications.markAllRead(accountId());
  }

  /** The event-type catalog with the account's opt-out state and each type's mandatory flag. */
  @GetMapping("/preferences")
  NotificationPreferencesView preferences() {
    return notifications.preferences(accountId());
  }

  /** Applies e-mail opt-out changes; opting out of a mandatory type → 422 (BR7). */
  @PutMapping("/preferences")
  NotificationPreferencesView updatePreferences(
      @Valid @RequestBody NotificationPreferencesRequest request) {
    return notifications.updatePreferences(accountId(), request.toUpdates());
  }

  private UUID accountId() {
    String email = userContext.current().username();
    return accounts
        .findByEmail(email)
        .map(AccountCredentials::accountId)
        .orElseThrow(
            () -> new IllegalStateException("authenticated user has no account: " + email));
  }

  private static int clampSize(int size) {
    return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
  }
}
