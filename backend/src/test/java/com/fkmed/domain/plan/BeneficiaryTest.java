package com.fkmed.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SPEC-0001 §Validation Rules + BR5: beneficiary invariants (domain/unit layer). */
class BeneficiaryTest {

  private static final String VALID_CPF = "52998224725";
  private static final String OTHER_VALID_CPF = "15350946056";
  private static final String VALID_CNS = "700000000000001";
  private static final LocalDate PAST = LocalDate.of(1988, 3, 12);

  private static Plan plan() {
    return Plan.create("PLANO TESTE", "326305", "ESTADUAL", true, true, List.of());
  }

  private static Beneficiary maria(Plan plan) {
    return Beneficiary.titular(
        plan, "MARIA CLARA SOUZA LIMA", VALID_CPF, VALID_CNS, "001234567", PAST);
  }

  @Test
  void titular_isCreatedActive_withoutTitularLink() {
    Beneficiary titular = maria(plan());
    assertThat(titular.getId()).isNotNull();
    assertThat(titular.getRole()).isEqualTo(BeneficiaryRole.TITULAR);
    assertThat(titular.getTitular()).isNull();
    assertThat(titular.isActive()).isTrue();
    assertThat(titular.getCardNumber()).isEqualTo("001234567");
  }

  @Test
  void dependent_isLinkedToTheTitular_andSharesThePlan() {
    Plan plan = plan();
    Beneficiary titular = maria(plan);
    Beneficiary dependent =
        Beneficiary.dependentOf(
            titular,
            "PEDRO SOUZA LIMA",
            OTHER_VALID_CPF,
            "700000000000002",
            "001234575",
            LocalDate.of(2007, 5, 20));
    assertThat(dependent.getRole()).isEqualTo(BeneficiaryRole.DEPENDENT);
    assertThat(dependent.getTitular()).isSameAs(titular);
    assertThat(dependent.getPlan()).isSameAs(plan);
  }

  @Test
  void dependentOfADependent_isRejected() {
    Beneficiary titular = maria(plan());
    Beneficiary dependent =
        Beneficiary.dependentOf(
            titular,
            "PEDRO SOUZA LIMA",
            OTHER_VALID_CPF,
            "700000000000002",
            "001234575",
            LocalDate.of(2007, 5, 20));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                Beneficiary.dependentOf(
                    dependent,
                    "NETO",
                    "39053344705",
                    "700000000000003",
                    "001234583",
                    LocalDate.of(2020, 1, 1)))
        .withMessageContaining("titular");
  }

  @ParameterizedTest
  @ValueSource(strings = {"12345678", "1234567890", "12345678X", ""})
  void cardNumber_mustHaveExactly9NumericDigits(String invalidCard) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> Beneficiary.titular(plan(), "MARIA", VALID_CPF, VALID_CNS, invalidCard, PAST));
  }

  @ParameterizedTest
  @ValueSource(strings = {"70000000000000", "7000000000000012", "70000000000000X"})
  void cns_mustHaveExactly15Digits(String invalidCns) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> Beneficiary.titular(plan(), "MARIA", VALID_CPF, invalidCns, "001234567", PAST));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "52998224724", // wrong second check digit
        "52998224715", // wrong first check digit
        "11111111111", // repeated digits
        "5299822472", // 10 digits
        "529982247251", // 12 digits
        "5299822472A" // non-numeric
      })
  void cpf_mustHaveValidCheckDigits(String invalidCpf) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> Beneficiary.titular(plan(), "MARIA", invalidCpf, VALID_CNS, "001234567", PAST));
  }

  @Test
  void birthDate_mustBeInThePast() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                Beneficiary.titular(
                    plan(),
                    "MARIA",
                    VALID_CPF,
                    VALID_CNS,
                    "001234567",
                    LocalDate.now().plusDays(1)));
  }

  @Test
  void fullName_isRequired() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> Beneficiary.titular(plan(), "  ", VALID_CPF, VALID_CNS, "001234567", PAST));
  }
}
