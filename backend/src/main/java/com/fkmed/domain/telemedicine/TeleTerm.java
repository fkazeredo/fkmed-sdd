package com.fkmed.domain.telemedicine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;

/**
 * A versioned teleattendance term (SPEC-0010 BR4). The current term is the most recently published
 * version; entering the queue requires accepting exactly that version, recorded on the session
 * ({@code term_version}). Seeded (and, in a real product, republished) by Flyway migration.
 */
@Entity
@Table(name = "tele_term")
@Getter
public class TeleTerm {

  @Id private String version;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  @Column(nullable = false)
  private String body;

  /** JPA only. */
  protected TeleTerm() {}
}
