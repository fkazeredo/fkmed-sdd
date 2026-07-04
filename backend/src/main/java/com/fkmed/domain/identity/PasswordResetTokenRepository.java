package com.fkmed.domain.identity;

import com.fkmed.domain.ModuleInternal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence of {@link PasswordResetToken}; internal to the identity module (baseline §0016). */
@ModuleInternal
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  /** Invalidates every still-open link of an account (a new request supersedes previous ones). */
  @Modifying
  @Query(
      "update PasswordResetToken t set t.usedAt = :now "
          + "where t.accountId = :accountId and t.usedAt is null")
  int invalidateOpenTokens(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
