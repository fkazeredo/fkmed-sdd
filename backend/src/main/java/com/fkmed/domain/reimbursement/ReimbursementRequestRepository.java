package com.fkmed.domain.reimbursement;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository of reimbursement requests. History/detail reads (SPEC-0016 API Contracts) are out of
 * this slice's scope; {@link #findByIdempotencyKey} backs the AC6 double-submit guard.
 */
public interface ReimbursementRequestRepository extends JpaRepository<ReimbursementRequest, UUID> {

  Optional<ReimbursementRequest> findByIdempotencyKey(String idempotencyKey);

  List<ReimbursementRequest> findByBeneficiaryIdInOrderByCreatedAtDesc(Collection<UUID> ids);

  Optional<ReimbursementRequest> findByIdAndBeneficiaryIdIn(UUID id, Collection<UUID> ids);

  List<ReimbursementRequest> findByStatusAndPendencyDeadlineAtBefore(
      ReimbursementStatus status, LocalDate date);
}
