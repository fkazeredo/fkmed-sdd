package com.fkmed.domain.appointment;

import java.util.UUID;

/** A care unit as shown in the wizard's unit step (SPEC-0009 BR3/BR4). */
public record CareUnitView(
    UUID id,
    String name,
    String cep,
    String street,
    String addressNumber,
    String complement,
    String neighborhood,
    String city,
    String uf,
    String phone) {

  static CareUnitView from(CareUnit unit) {
    return new CareUnitView(
        unit.getId(),
        unit.getName(),
        unit.getCep(),
        unit.getStreet(),
        unit.getAddressNumber(),
        unit.getComplement(),
        unit.getNeighborhood(),
        unit.getCity(),
        unit.getUf(),
        unit.getPhone());
  }
}
