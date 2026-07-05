/**
 * The digital-card module (SPEC-0007): the beneficiary's card + data sheet and its PDF download.
 *
 * <p>Owns no tables of its own — it reads the existing {@code plan}/{@code beneficiary} registry
 * (extended in place by Flyway V9 with {@code plan.category}) through {@code domain.plan}'s
 * existing family-scope facade ({@link com.fkmed.domain.plan.BeneficiaryAccess#cardDetailsFor}),
 * reusing SPEC-0003's scoping rather than reimplementing it. Depends on {@code domain.identity}'s
 * {@link com.fkmed.domain.identity.IdentityAccounts} to resolve the acting account for the BR7
 * audit entry, and on {@code domain.audit}'s {@link com.fkmed.domain.audit.AuditRecorder} to record
 * it. Module map: ADR-0001 (revised by ADR-0007).
 */
@org.springframework.modulith.ApplicationModule(displayName = "card")
package com.fkmed.domain.card;
