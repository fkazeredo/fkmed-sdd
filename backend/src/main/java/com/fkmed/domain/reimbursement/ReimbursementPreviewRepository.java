package com.fkmed.domain.reimbursement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for reimbursement previews. */
public interface ReimbursementPreviewRepository extends JpaRepository<ReimbursementPreview, UUID> {

  List<ReimbursementPreview> findByBeneficiaryIdInOrderByCreatedAtDesc(Collection<UUID> ids);

  Optional<ReimbursementPreview> findByIdAndBeneficiaryIdIn(UUID id, Collection<UUID> ids);
}
