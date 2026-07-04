package com.fkmed.domain.identity;

/**
 * Port over the curated common-passwords denylist (SPEC-0002 BR9). The infra adapter loads the
 * seeded resource; the domain checks membership without knowing the source.
 */
public interface CommonPasswordDenylist {

  /** True when the password appears in the denylist (case-insensitive). */
  boolean contains(String password);
}
