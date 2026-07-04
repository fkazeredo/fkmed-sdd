package com.fkmed.domain.plan;

import com.fkmed.domain.ModuleInternal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link Beneficiary}; internal to the plan module (DECISIONS-BASELINE §0016). */
@ModuleInternal
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

  Optional<Beneficiary> findByCardNumberAndActiveTrue(String cardNumber);

  List<Beneficiary> findByTitularIdAndActiveTrueOrderByBirthDate(UUID titularId);

  Optional<Beneficiary> findByIdAndActiveTrue(UUID id);

  Optional<Beneficiary> findByCpfAndCardNumberAndBirthDateAndActiveTrue(
      String cpf, String cardNumber, LocalDate birthDate);
}
