package com.fkmed.domain.reimbursement;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository of the plan reimbursement table (SPEC-0015 §Persistence). Not read by this slice's own
 * service (submission does not calculate a reimbursed value — that is SPEC-0016 BR2/BR3); seeded
 * now so SPEC-0016's analysis engine has the table ready.
 */
public interface ReimbursementTableRepository
    extends JpaRepository<ReimbursementTableEntry, String> {}
