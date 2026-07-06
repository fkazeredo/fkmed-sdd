package com.fkmed.domain.notification;

import java.util.List;

/** The full per-event-type preferences catalog for an account (SPEC-0004 §API Contracts). */
public record NotificationPreferencesView(List<NotificationPreferenceView> preferences) {}
