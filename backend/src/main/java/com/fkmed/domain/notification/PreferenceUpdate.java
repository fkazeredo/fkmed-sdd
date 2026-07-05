package com.fkmed.domain.notification;

/**
 * One requested preference change (SPEC-0004 §API Contracts): opt the account's e-mail channel for
 * {@code type} in or out. Unknown types are ignored; opting out of a mandatory type is rejected.
 */
public record PreferenceUpdate(String type, boolean emailOptOut) {}
