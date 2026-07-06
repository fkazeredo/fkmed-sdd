/**
 * The support module (SPEC-0014): the Canais de Atendimento screen — operator-managed contact
 * channels (BR1/BR2), the antifraud section (BR3, the destination of the Home fraud banner —
 * SPEC-0005 BR9/AC6), the Central de Libras service request (BR4) and the searchable FAQ (BR5/BR6).
 *
 * <p>Owns the {@code support_channel}, {@code faq_entry} and {@code libras_request} tables (Flyway
 * V25) plus the BR1/BR6 seed. Channels and FAQ are operator-loaded reference content in this phase
 * (no CMS yet, mirroring {@code domain.content}); the antifraud copy is fixed seeded text served
 * without a dedicated table (Rule Zero). The Libras request is the module's only write path: scope
 * check reuses {@code domain.plan.BeneficiaryAccess} (SPEC-0003 BR2/BR3), and every registration is
 * audited via {@code domain.audit.AuditRecorder} (SPEC-0003 BR6) and counted via {@link
 * io.micrometer.core.instrument.MeterRegistry}, mirroring {@code domain.guides.TokenService}.
 * Module map: ADR-0019.
 */
@org.springframework.modulith.ApplicationModule(displayName = "support")
package com.fkmed.domain.support;
