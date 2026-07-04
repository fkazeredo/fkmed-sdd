package com.fkmed.infra.security;

/**
 * Port giving delivery code access to the authenticated user's identity, decoupled from Spring
 * Security internals (see {@code docs/architecture/security.md} §Current user context).
 */
public interface UserContextProvider {

  /** The current authenticated user; only callable on authenticated routes. */
  UserContext current();
}
