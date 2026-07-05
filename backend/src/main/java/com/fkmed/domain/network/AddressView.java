package com.fkmed.domain.network;

/**
 * A provider's full address (SPEC-0008 BR12): backs the "Traçar rota" (opens the address in the
 * maps service) and "Copiar endereço" actions, both implemented client-side.
 */
public record AddressView(
    String cep,
    String street,
    String number,
    String complement,
    String neighborhood,
    String municipality,
    String uf) {}
