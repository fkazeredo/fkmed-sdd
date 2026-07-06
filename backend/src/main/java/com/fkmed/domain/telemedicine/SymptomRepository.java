package com.fkmed.domain.telemedicine;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read/validate access to the {@code symptom} registry (SPEC-0010 BR2). */
public interface SymptomRepository extends JpaRepository<Symptom, String> {

  /** The catalog for the triage screen, alphabetical by name. */
  List<Symptom> findAllByOrderByNameAsc();
}
