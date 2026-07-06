package com.fkmed.domain.clinicaldocs;

/**
 * One prescribed medication supplied to the issuance facade (SPEC-0011 BR6): medication, posology
 * and guidance.
 */
public record PrescriptionItemInput(String medication, String posology, String guidance) {}
