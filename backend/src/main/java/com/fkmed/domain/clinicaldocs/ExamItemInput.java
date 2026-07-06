package com.fkmed.domain.clinicaldocs;

/** One requested exam supplied to the issuance facade (SPEC-0011 BR6): name + TUSS code. */
public record ExamItemInput(String examName, String tussCode) {}
