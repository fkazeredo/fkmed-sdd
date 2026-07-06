package com.fkmed.domain.plan;

import com.fkmed.domain.ModuleInternal;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of the {@link UfRegistryEntry} registry; internal to the plan module (§0016). */
@ModuleInternal
public interface UfRegistryRepository extends JpaRepository<UfRegistryEntry, String> {}
