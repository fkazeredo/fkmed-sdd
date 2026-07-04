package com.fkmed.domain.plan;

import com.fkmed.domain.ModuleInternal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Plan}; internal to the plan module (DECISIONS-BASELINE §0016). */
@ModuleInternal
public interface PlanRepository extends JpaRepository<Plan, UUID> {}
