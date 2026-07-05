package com.fkmed.domain.content;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service of the content module: resolves the Home content view (SPEC-0005 BR6/BR7).
 *
 * <p>Public module facade — the only entry point the delivery layer (and any future module) should
 * use to read Home content.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeContent {

  private final BannerRepository banners;
  private final NoticeRepository notices;
  private final Clock clock;

  /**
   * Resolves the banners currently visible (active + inside their validity window, SPEC-0005 BR6)
   * and the active notices (BR7), both in content-defined order. The validity check runs against
   * the injected {@link Clock}, so it is fully deterministic in tests.
   */
  public HomeContentResponse home() {
    Instant now = clock.instant();
    var visibleBanners =
        banners.findAllByOrderByDisplayOrderAsc().stream().filter(b -> b.isVisibleAt(now)).toList();
    var activeNotices = notices.findByActiveTrueOrderByDisplayOrderAsc();
    return HomeContentResponse.of(visibleBanners, activeNotices);
  }
}
