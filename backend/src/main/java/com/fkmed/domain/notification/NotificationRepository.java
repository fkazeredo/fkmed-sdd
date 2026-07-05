package com.fkmed.domain.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence port for {@link Notification} — account-scoped listing, unread count and reads. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** One page of the account's notifications, newest first (SPEC-0004 BR3). */
  List<Notification> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

  /** The account's unread count for the bell counter (BR2). */
  int countByAccountIdAndReadAtIsNull(UUID accountId);

  /** A single notification owned by the account, or empty when unknown/foreign (BR — 404). */
  Optional<Notification> findByIdAndAccountId(UUID id, UUID accountId);

  /** Marks every unread notification of the account read at {@code when}; returns rows touched. */
  @Modifying
  @Query(
      "update Notification n set n.readAt = :when"
          + " where n.accountId = :accountId and n.readAt is null")
  int markAllReadFor(@Param("accountId") UUID accountId, @Param("when") Instant when);
}
