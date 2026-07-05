package com.fkmed.domain.notification;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for the {@link NotificationEventType} registry catalog (seeded by V10). */
public interface NotificationEventTypeRepository
    extends JpaRepository<NotificationEventType, String> {}
