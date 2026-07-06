package com.fkmed.domain.support;

import com.fkmed.domain.ModuleInternal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence of {@link LibrasRequest}; internal to the support module (DECISIONS-BASELINE §0016).
 */
@ModuleInternal
public interface LibrasRequestRepository extends JpaRepository<LibrasRequest, UUID> {}
