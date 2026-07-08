package com.fkmed.domain.reimbursement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only repository of the expense-type registry (SPEC-0015 BR4). */
public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, String> {

  List<ExpenseType> findAllByOrderByCodeAsc();
}
