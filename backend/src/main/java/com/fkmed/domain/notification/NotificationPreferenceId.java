package com.fkmed.domain.notification;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Composite key of {@link NotificationPreference}: one preference per (account, event type). JPA
 * {@code @IdClass} — needs a no-arg constructor and value equality (Lombok-generated).
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NotificationPreferenceId implements Serializable {

  private UUID accountId;
  private String eventTypeCode;
}
