package com.fkmed.domain.content;

import java.util.List;

/**
 * View returned by {@link HomeContent#home()} and rendered by the Home screen (SPEC-0005 BR6/BR7).
 * Response records that map an entity stay inside their domain module; the delivery layer is
 * entity-free. An empty list in either section means the frontend hides that section (BR8) — it is
 * not an error.
 */
public record HomeContentResponse(List<BannerView> banners, List<NoticeView> notices) {

  /** A banner already filtered to be visible now (SPEC-0005 BR6). */
  public record BannerView(
      String title,
      String text,
      String buttonLabel,
      String destination,
      String imageUrl,
      int order) {}

  /** A notice already filtered to be active (SPEC-0005 BR7). */
  public record NoticeView(String title, NoticeSeverity severity, String body, int order) {}

  /** Maps the already-filtered, already-ordered entities into the view. */
  public static HomeContentResponse of(List<Banner> banners, List<Notice> notices) {
    return new HomeContentResponse(
        banners.stream()
            .map(
                b ->
                    new BannerView(
                        b.getTitle(),
                        b.getText(),
                        b.getButtonLabel(),
                        b.getInternalDestination(),
                        b.getImage(),
                        b.getDisplayOrder()))
            .toList(),
        notices.stream()
            .map(
                n ->
                    new NoticeView(n.getTitle(), n.getSeverity(), n.getBody(), n.getDisplayOrder()))
            .toList());
  }
}
