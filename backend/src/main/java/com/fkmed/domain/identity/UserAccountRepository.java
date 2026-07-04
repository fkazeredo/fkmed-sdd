package com.fkmed.domain.identity;

import com.fkmed.domain.ModuleInternal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link UserAccount}; internal to the identity module (baseline §0016). */
@ModuleInternal
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByBeneficiaryId(UUID beneficiaryId);
}
