package com.fkmed.domain.support;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only repository of the channel cards (SPEC-0014 BR1), ordered for display. */
public interface SupportChannelRepository extends JpaRepository<SupportChannel, UUID> {

  List<SupportChannel> findAllByOrderByDisplayOrderAsc();
}
