package com.fkmed.domain.support;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of registered Libras service requests (SPEC-0014 BR4). */
public interface LibrasRequestRepository extends JpaRepository<LibrasRequest, UUID> {}
