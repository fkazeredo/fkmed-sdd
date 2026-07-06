package com.fkmed.domain.appointment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Care-unit lookups (SPEC-0009 §units endpoint). */
interface CareUnitRepository extends JpaRepository<CareUnit, UUID> {

  /** The seeded virtual Telemedicina unit backing the tele agenda (SPEC-0010 BR14, DL-0018). */
  Optional<CareUnit> findFirstByVirtualTrue();

  /**
   * The active units that serve a given scope (specialty or exam), alphabetical by name (BR3/BR4).
   */
  @Query(
      "select u from CareUnit u where u.id in ("
          + " select a.unitId from UnitAgenda a"
          + " where a.scopeType = :scopeType and a.scopeCode = :scopeCode)"
          + " order by u.name")
  List<CareUnit> findServingScope(
      @Param("scopeType") AppointmentType scopeType, @Param("scopeCode") String scopeCode);
}
