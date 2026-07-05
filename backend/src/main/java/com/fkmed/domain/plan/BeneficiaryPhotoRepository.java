package com.fkmed.domain.plan;

import com.fkmed.domain.ModuleInternal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link BeneficiaryPhoto}; internal to the plan module (§0016). */
@ModuleInternal
public interface BeneficiaryPhotoRepository extends JpaRepository<BeneficiaryPhoto, UUID> {

  boolean existsByBeneficiaryId(UUID beneficiaryId);
}
