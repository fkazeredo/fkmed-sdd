package com.fkmed.domain.reimbursement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only repository of the professional-council registry (SPEC-0015 BR10). */
public interface ProfessionalCouncilRepository extends JpaRepository<ProfessionalCouncil, String> {

  List<ProfessionalCouncil> findAllByOrderByCodeAsc();
}
