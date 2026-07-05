package com.fkmed.domain.notification;

import java.util.UUID;

/**
 * A request to raise a notification (SPEC-0004). The in-app item is always created; e-mail is sent
 * only when the type/preference resolution says so AND a {@code recipientEmail} is present. {@code
 * title}/{@code body} MUST already be masked (BR4 — no full CPF/CNS/bank data).
 *
 * @param accountId the recipient account.
 * @param eventTypeCode the (seeded) event type code.
 * @param title the in-app title (≤ 120 chars).
 * @param body the in-app body (≤ 500 chars).
 * @param link an optional deep link into the portal, or null.
 * @param recipientEmail the delivery e-mail for the e-mail channel, or null to never e-mail.
 */
public record NotificationRequest(
    UUID accountId,
    String eventTypeCode,
    String title,
    String body,
    String link,
    String recipientEmail) {}
