package com.fkmed.domain.notification;

import java.util.List;

/**
 * The paginated notification list response (SPEC-0004 §Input/Output): the account's total unread
 * count (BR2) plus one page of items, newest first (BR3).
 */
public record NotificationListView(int unread, List<NotificationView> items) {}
