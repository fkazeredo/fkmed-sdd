package com.fkmed.domain.reimbursement;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of the reimbursement adhesion-term versions (SPEC-0015 BR3). */
public interface AdhesionTermRepository extends JpaRepository<AdhesionTerm, java.util.UUID> {

  /** The single current version (POC: exactly one seeded row — {@link #findAll} would also do). */
  Optional<AdhesionTerm> findTopByOrderByPublishedAtDesc();
}
