package com.fkmed.domain.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

/** Exam-catalog registry access (SPEC-0009 BR4). */
interface ExamTypeRepository extends JpaRepository<ExamType, String> {}
