/**
 * The content module (SPEC-0005): operator-managed Home content — banners with an optional validity
 * window (BR6) and notices (BR7) — surfaced by the {@code GET /api/content/home} read model.
 *
 * <p>Owns the {@code banner} and {@code notice} tables (Flyway V8) plus the BR9 seed. Content is
 * operator-loaded via migration in this phase (no CMS yet); visibility filtering (the {@code
 * active} flag and the banner validity window) happens server-side in {@link
 * com.fkmed.domain.content.HomeContent}, fully test-controllable through an injected {@link
 * java.time.Clock} — never by hiding rows at the database layer. Depends on no other business
 * module. Module map: ADR-0001 (revised by ADR-0006).
 */
@org.springframework.modulith.ApplicationModule(displayName = "content")
package com.fkmed.domain.content;
