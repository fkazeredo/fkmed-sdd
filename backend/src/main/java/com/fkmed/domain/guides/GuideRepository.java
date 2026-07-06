package com.fkmed.domain.guides;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Command-and-read repository of the guide aggregate (SPEC-0012). */
public interface GuideRepository extends JpaRepository<Guide, UUID> {

  /** A beneficiary's guides, most-recent-first (BR2). */
  List<Guide> findByBeneficiaryIdOrderByRequestedAtDesc(UUID beneficiaryId);
}
