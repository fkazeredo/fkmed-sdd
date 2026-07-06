package com.fkmed.domain.support;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service and public facade for the channel cards (SPEC-0014 BR1/BR2): read-only,
 * exclusively from the operator-managed registry (BR2 — never a hardcoded contact).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportChannels {

  private final SupportChannelRepository channels;

  /** The channel cards in content-defined order (BR1). */
  public List<SupportChannelView> list() {
    return channels.findAllByOrderByDisplayOrderAsc().stream()
        .map(
            c ->
                new SupportChannelView(
                    c.getType(), c.getLabel(), c.getValue(), c.getHours(), c.getDisplayOrder()))
        .toList();
  }
}
