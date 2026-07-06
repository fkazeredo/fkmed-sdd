package com.fkmed.domain.network;

import java.util.List;
import java.util.UUID;

/**
 * A provider's detail screen (SPEC-0008 BR12): name, service type, specialties, full address, phone
 * and seals with name/description.
 */
public record ProviderDetailResponse(
    UUID id,
    String name,
    String serviceType,
    List<String> specialties,
    AddressView address,
    String phone,
    List<SealBadge> seals) {}
