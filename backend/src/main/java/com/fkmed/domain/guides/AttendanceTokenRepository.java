package com.fkmed.domain.guides;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Command-and-read repository of the attendance-token aggregate (SPEC-0012 BR9). */
public interface AttendanceTokenRepository extends JpaRepository<AttendanceToken, UUID> {

  /** The beneficiary's current non-invalidated token, if any (mirrors the partial unique index). */
  Optional<AttendanceToken> findByBeneficiaryIdAndInvalidatedAtIsNull(UUID beneficiaryId);
}
