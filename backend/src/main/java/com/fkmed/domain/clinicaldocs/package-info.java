/**
 * The clinical-documents module (SPEC-0011 — Minha Saúde): the beneficiary's digital clinical
 * documents (exam orders, referrals, prescriptions, sick notes), read-only for beneficiaries.
 *
 * <p>Owns {@code clinical_document} (the immutable header + type-specific fields for the
 * single-valued types — referral, sick note) plus the {@code exam_order_item}/{@code
 * prescription_item} child rows for the list-valued types. Documents are created ONLY through the
 * internal issuance facade {@link com.fkmed.domain.clinicaldocs.ClinicalDocuments#issue}, called by
 * the telemedicine closure (SPEC-0010 BR10) and the operator simulation (SPEC-0018) in Wave 2 — no
 * beneficiary write path exists (BR8 immutability). Family scope and the dependent-access audit
 * reuse {@code domain.plan}'s {@link com.fkmed.domain.plan.BeneficiaryAccess} and {@code
 * domain.audit}'s {@link com.fkmed.domain.audit.AuditRecorder} rather than reimplementing them
 * (ADR-0013). PDF rendering reuses the OpenPDF setup of {@code domain.card} (ADR-0007).
 *
 * <p>Module map: ADR-0001, revised by ADR-0013 (10th verified module).
 */
@org.springframework.modulith.ApplicationModule(displayName = "clinicaldocs")
package com.fkmed.domain.clinicaldocs;
