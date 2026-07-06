package com.fkmed.domain.appointment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Agenda (unit, scope) lookups. */
interface UnitAgendaRepository extends JpaRepository<UnitAgenda, UUID> {

  Optional<UnitAgenda> findByUnitIdAndScopeTypeAndScopeCode(
      UUID unitId, AppointmentType scopeType, String scopeCode);
}
