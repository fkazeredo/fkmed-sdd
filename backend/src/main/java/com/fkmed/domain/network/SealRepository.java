package com.fkmed.domain.network;

import com.fkmed.domain.ModuleInternal;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of the {@link Seal} registry; internal to the network module (§0016). */
@ModuleInternal
public interface SealRepository extends JpaRepository<Seal, String> {}
