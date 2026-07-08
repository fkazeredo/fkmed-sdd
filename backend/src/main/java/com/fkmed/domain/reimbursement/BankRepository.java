package com.fkmed.domain.reimbursement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only repository of the bank registry (SPEC-0015 BR11). */
public interface BankRepository extends JpaRepository<Bank, String> {

  List<Bank> findAllByOrderByNameAsc();
}
