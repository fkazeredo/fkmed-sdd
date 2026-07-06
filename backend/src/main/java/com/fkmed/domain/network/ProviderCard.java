package com.fkmed.domain.network;

import java.util.List;
import java.util.UUID;

/**
 * A provider result card (SPEC-0008 BR7): name, the {@code "BAIRRO, MUNICÍPIO – UF"} locality
 * label, its service type (BR8: name search shows it since results may span types funnel results
 * never would) and seals when present.
 */
public record ProviderCard(
    UUID id, String name, String locality, String serviceType, List<SealBadge> seals) {}
