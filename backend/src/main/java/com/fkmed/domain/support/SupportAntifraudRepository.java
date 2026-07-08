package com.fkmed.domain.support;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read-only repository of the single-row antifraud content (SPEC-0014 BR3). */
public interface SupportAntifraudRepository extends JpaRepository<SupportAntifraud, UUID> {}
