package com.fkmed.domain.content;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Banner}; internal to the content module (DECISIONS-BASELINE §0016). */
@ModuleInternal
public interface BannerRepository extends JpaRepository<Banner, UUID> {

  /**
   * All banners in content-defined order (SPEC-0005 BR6); visibility (active + validity window) is
   * filtered afterwards via {@link Banner#isVisibleAt}, not by this query.
   */
  List<Banner> findAllByOrderByDisplayOrderAsc();
}
