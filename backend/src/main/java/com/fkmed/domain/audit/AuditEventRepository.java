package com.fkmed.domain.audit;

import com.fkmed.domain.ModuleInternal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence of {@link AuditEvent}; internal to the audit module (DECISIONS-BASELINE §0016).
 * Deliberately exposes no update and no record-level delete — only append, read and the coarse
 * time-window purge of the 12-month retention (SPEC-0003 BR7/BR10).
 */
@ModuleInternal
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  List<AuditEvent> findByEventType(String eventType);

  List<AuditEvent> findByTargetBeneficiaryIdOrderByOccurredAtDesc(UUID targetBeneficiaryId);

  /** Bulk retention sweep (BR10): deletes every entry strictly older than the cutoff. */
  @Modifying
  @Query("delete from AuditEvent a where a.occurredAt < :cutoff")
  int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
