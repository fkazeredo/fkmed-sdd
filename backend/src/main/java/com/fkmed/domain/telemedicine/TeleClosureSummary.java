package com.fkmed.domain.telemedicine;

/**
 * The professional's closing summary passed to {@link TeleService#close} (SPEC-0010 BR9). Wave 2's
 * operator simulation supplies it; the clinical-document issuance is wired off the resulting {@link
 * TeleSessionClosed} event.
 */
public record TeleClosureSummary(
    String professionalName, String professionalCrm, String guidance) {}
