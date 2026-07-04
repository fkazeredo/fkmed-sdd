package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** SPEC-0001 BR6: resolution of the "my plan" view (domain/unit layer). */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

  @Mock private BeneficiaryRepository beneficiaries;

  private PlanService service;

  private Plan plan;
  private Beneficiary maria;
  private Beneficiary pedro;

  @BeforeEach
  void setUp() {
    service = new PlanService(beneficiaries);
    plan =
        Plan.create(
            "PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP",
            "326305",
            "ESTADUAL",
            true,
            true,
            List.of("Urg/emerg Nacional Hr — Assistência"));
    maria =
        Beneficiary.titular(
            plan,
            "MARIA CLARA SOUZA LIMA",
            "52998224725",
            "700000000000001",
            "001234567",
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
  void nullCard_throwsPlanNotFound() {
    assertThatExceptionOfType(PlanNotFoundException.class)
        .isThrownBy(() -> service.myPlanFor(null))
        .satisfies(e -> assertThat(e.getCode()).isEqualTo("plan.not-found"));
  }

  @Test
  void unknownCard_throwsPlanNotFound() {
    when(beneficiaries.findByCardNumberAndActiveTrue("999999999")).thenReturn(Optional.empty());
    assertThatExceptionOfType(PlanNotFoundException.class)
        .isThrownBy(() -> service.myPlanFor("999999999"));
  }

  @Test
  void titularCard_returnsPlanData_andMembersWithTitularFirst() {
    when(beneficiaries.findByCardNumberAndActiveTrue("001234567")).thenReturn(Optional.of(maria));
    when(beneficiaries.findByTitularIdAndActiveTrueOrderByBirthDate(maria.getId()))
        .thenReturn(List.of(pedro));

    MyPlanResponse response = service.myPlanFor("001234567");

    assertThat(response.plan().name()).isEqualTo("PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP");
    assertThat(response.plan().ansRegistration()).isEqualTo("326305");
    assertThat(response.plan().coverage()).isEqualTo("ESTADUAL");
    assertThat(response.plan().copay()).isTrue();
    assertThat(response.plan().reimbursement()).isTrue();
    assertThat(response.plan().additives()).containsExactly("Urg/emerg Nacional Hr — Assistência");
    assertThat(response.members())
        .extracting(MyPlanResponse.MemberSummary::fullName)
        .containsExactly("MARIA CLARA SOUZA LIMA", "PEDRO SOUZA LIMA");
    assertThat(response.members())
        .extracting(MyPlanResponse.MemberSummary::role)
        .containsExactly(BeneficiaryRole.TITULAR, BeneficiaryRole.DEPENDENT);
    assertThat(response.members())
        .extracting(MyPlanResponse.MemberSummary::cardNumber)
        .containsExactly("001234567", "001234575");
  }

  @Test
  void dependentCard_resolvesTheFamilyThroughTheTitular() {
    when(beneficiaries.findByCardNumberAndActiveTrue("001234575")).thenReturn(Optional.of(pedro));
    when(beneficiaries.findByTitularIdAndActiveTrueOrderByBirthDate(maria.getId()))
        .thenReturn(List.of(pedro));

    MyPlanResponse response = service.myPlanFor("001234575");

    assertThat(response.members())
        .extracting(MyPlanResponse.MemberSummary::cardNumber)
        .containsExactly("001234567", "001234575");
  }
}
