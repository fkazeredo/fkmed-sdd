package com.fkmed.domain.network;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of the {@link ServiceType} registry; internal to the network module (§0016). */
@ModuleInternal
public interface ServiceTypeRepository extends JpaRepository<ServiceType, String> {

  /** The catalog in its fixed BR5 display order. */
  List<ServiceType> findAllByOrderBySortOrderAsc();
}
