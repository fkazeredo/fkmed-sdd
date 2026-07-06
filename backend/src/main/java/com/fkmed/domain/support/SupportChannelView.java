package com.fkmed.domain.support;

/** One support channel card (SPEC-0014 BR1), in content-defined order. */
public record SupportChannelView(
    ChannelType type, String label, String value, String hours, int displayOrder) {}
