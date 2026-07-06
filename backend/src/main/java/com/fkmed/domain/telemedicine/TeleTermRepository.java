package com.fkmed.domain.telemedicine;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Access to the versioned {@code tele_term} (SPEC-0010 BR4). */
public interface TeleTermRepository extends JpaRepository<TeleTerm, String> {

  /** The current term: the most recently published version. */
  Optional<TeleTerm> findTopByOrderByPublishedAtDesc();
}
