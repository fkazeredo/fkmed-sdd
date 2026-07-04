package com.fkmed.domain.identity;

import com.fkmed.domain.ModuleInternal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence of {@link EmailVerificationToken}; internal to the identity module (§0016). */
@ModuleInternal
public interface EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationToken, UUID> {

  Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

  /** Invalidates every still-open link of an account (resend supersedes previous links, BR5). */
  @Modifying
  @Query(
      "update EmailVerificationToken t set t.usedAt = :now "
          + "where t.accountId = :accountId and t.usedAt is null")
  int invalidateOpenTokens(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
