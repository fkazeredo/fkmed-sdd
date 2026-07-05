package com.fkmed.domain.notification;

/**
 * One row of the preferences catalog (SPEC-0004 §API Contracts): the event {@code type} code, its
 * pt-BR {@code description} label, the user's current {@code emailOptOut} state and whether the
 * type is {@code mandatory} (its e-mail cannot be disabled, BR7).
 */
public record NotificationPreferenceView(
    String type, String description, boolean emailOptOut, boolean mandatory) {}
