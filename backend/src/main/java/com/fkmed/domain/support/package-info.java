/**
 * The support module (SPEC-0014 — Canais de Atendimento e FAQ): operator-managed channel cards
 * (BR1/BR2), the antifraud section content (BR3, destination of the Home fraud banner — SPEC-0005
 * BR9), Libras service-request registration (BR4) and the searchable FAQ (BR5/BR6).
 *
 * <p>Content-serving and mostly read-only — the ONLY write path is {@link
 * com.fkmed.domain.support.SupportService#requestLibras}, scope-checked through {@code
 * domain.plan}'s {@link com.fkmed.domain.plan.BeneficiaryAccess} the same way {@code
 * domain.guides.TokenService} scope-checks token generation. Depends on {@code domain.plan} (family
 * scope), {@code domain.identity} (author-account resolution) and {@code domain.audit} (the
 * Libras-request audit entry, SPEC-0014 §Observability).
 *
 * <p>Module map: ADR-0021 (14th verified module).
 */
@org.springframework.modulith.ApplicationModule(displayName = "support")
package com.fkmed.domain.support;
