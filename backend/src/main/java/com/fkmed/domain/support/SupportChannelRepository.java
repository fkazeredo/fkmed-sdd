package com.fkmed.domain.support;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link SupportChannel}; internal to the support module (DECISIONS-BASELINE §0016).
 */
@ModuleInternal
public interface SupportChannelRepository extends JpaRepository<SupportChannel, UUID> {

  /** The channel cards in content-defined order (SPEC-0014 BR1). */
  List<SupportChannel> findAllByOrderByDisplayOrderAsc();
}
