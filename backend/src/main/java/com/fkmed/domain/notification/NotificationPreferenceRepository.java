package com.fkmed.domain.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for the per-account {@link NotificationPreference} rows. */
public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

  /** Every preference row of the account (to overlay opt-outs on the catalog). */
  List<NotificationPreference> findByAccountId(UUID accountId);

  /** The account's preference for one event type, when set. */
  Optional<NotificationPreference> findByAccountIdAndEventTypeCode(
      UUID accountId, String eventTypeCode);
}
