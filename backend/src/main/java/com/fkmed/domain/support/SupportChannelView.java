package com.fkmed.domain.support;

/**
 * One channel card (SPEC-0014 BR1) — {@code type} is a {@link ChannelTypeCodes} code; {@code
 * sublabel} is present only when a type has more than one row (Central 24h).
 */
public record SupportChannelView(
    String type, String label, String sublabel, String value, String hours, int order) {}
