package com.fkmed.infra.email;

/**
 * A plain-text e-mail to deliver.
 *
 * @param to recipient address.
 * @param subject subject line.
 * @param body plain-text body.
 */
public record MailMessage(String to, String subject, String body) {}
