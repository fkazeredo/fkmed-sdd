package com.fkmed.domain.network;

import com.fkmed.domain.ModuleInternal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of the {@link Specialty} registry; internal to the network module (§0016). Cross-
 * module reuse (SPEC-0009's {@code domain.appointment}) goes through the {@link NetworkSpecialties}
 * public facade, never this repository directly (ADR-0011 Wave 2 freeze).
 */
@ModuleInternal
public interface SpecialtyRepository extends JpaRepository<Specialty, String> {}
