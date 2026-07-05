package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SPEC-0003 family-scope authorization (domain/unit layer) plus SPEC-0007's {@link
 * BeneficiaryAccess#cardDetailsFor} — the masking boundary that lets CNS leave the plan module in
 * full ONLY through this one method (BR8).
 */
@ExtendWith(MockitoExtension.class)
class BeneficiaryAccessTest {

  private static final String MARIA_CARD = "001234567";

  @Mock private BeneficiaryRepository beneficiaries;

  private BeneficiaryAccess access;
  private Plan plan;
  private Beneficiary maria;
  private Beneficiary pedro;

  @BeforeEach
  void setUp() {
    access = new BeneficiaryAccess(beneficiaries);
    plan =
        Plan.create(
            "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
            "326305",
            "ESTADUAL",
            "Coletivo por Adesão",
            true,
            true,
            List.of("Urg/emerg Nacional Hr — Assistência"));
    maria =
        Beneficiary.titular(
            plan,
            "MARIA CLARA SOUZA LIMA",
            "52998224725",
            "700000000000001",
            MARIA_CARD,
            LocalDate.of(1988, 3, 12));
    pedro =
        Beneficiary.dependentOf(
            maria,
            "PEDRO SOUZA LIMA",
            "15350946056",
            "700000000000002",
            "001234575",
            LocalDate.of(2007, 5, 20));
  }

  @Test
  void cardDetailsFor_callerViewingThemselves_isNotFlaggedAsDependentView() {
    when(beneficiaries.findByCardNumberAndActiveTrue(MARIA_CARD)).thenReturn(Optional.of(maria));

    CardDetails details = access.cardDetailsFor(MARIA_CARD, maria.getId());

    assertThat(details.fullName()).isEqualTo("MARIA CLARA SOUZA LIMA");
    assertThat(details.cns()).isEqualTo("700000000000001");
    assertThat(details.active()).isTrue();
    assertThat(details.viewedAsDependent()).isFalse();
    assertThat(details.planCategory()).isEqualTo("Coletivo por Adesão");
    assertThat(details.additives()).containsExactly("Urg/emerg Nacional Hr — Assistência");
  }

  @Test
  void cardDetailsFor_titularViewingAnActiveDependent_returnsCnsInFull_flaggedAsDependentView() {
    when(beneficiaries.findByCardNumberAndActiveTrue(MARIA_CARD)).thenReturn(Optional.of(maria));
    when(beneficiaries.findByTitularIdOrderByBirthDate(maria.getId())).thenReturn(List.of(pedro));

    CardDetails details = access.cardDetailsFor(MARIA_CARD, pedro.getId());

    assertThat(details.fullName()).isEqualTo("PEDRO SOUZA LIMA");
    assertThat(details.cns()).isEqualTo("700000000000002");
    assertThat(details.active()).isTrue();
    assertThat(details.viewedAsDependent()).isTrue();
  }

  @Test
  void cardDetailsFor_titularViewingAnInactiveDependent_stillFoundInScope_reportedInactive() {
    pedro.deactivate();
    when(beneficiaries.findByCardNumberAndActiveTrue(MARIA_CARD)).thenReturn(Optional.of(maria));
    when(beneficiaries.findByTitularIdOrderByBirthDate(maria.getId())).thenReturn(List.of(pedro));

    // Unlike accessibleFor/summaryFor (which exclude inactive dependents entirely), this method
    // must still resolve the beneficiary within scope so the card module can answer 409
    // card.unavailable (BR10) rather than a 404 that would incorrectly suggest out-of-scope.
    CardDetails details = access.cardDetailsFor(MARIA_CARD, pedro.getId());

    assertThat(details.active()).isFalse();
  }

  @Test
  void cardDetailsFor_targetOutsideCallerScope_throwsBeneficiaryNotAccessible() {
    UUID someoneElseId = UUID.randomUUID();
    when(beneficiaries.findByCardNumberAndActiveTrue(MARIA_CARD)).thenReturn(Optional.of(maria));
    when(beneficiaries.findByTitularIdOrderByBirthDate(maria.getId())).thenReturn(List.of(pedro));

    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(() -> access.cardDetailsFor(MARIA_CARD, someoneElseId));
  }

  @Test
  void cardDetailsFor_absentCallerCard_throwsBeneficiaryNotAccessible() {
    assertThatExceptionOfType(BeneficiaryNotAccessibleException.class)
        .isThrownBy(() -> access.cardDetailsFor(null, maria.getId()));
  }
}
