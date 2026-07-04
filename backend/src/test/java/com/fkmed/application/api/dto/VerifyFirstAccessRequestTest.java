package com.fkmed.application.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0002 BR1: {@code cpf}/{@code cardNumber} MUST be {@code @NotBlank} — consistent with the
 * sibling request fields ({@code email}/{@code password}/{@code token}, all {@code @NotBlank}).
 * Before this fix, a {@code null} value skipped {@code @Pattern} entirely (Bean Validation treats
 * {@code null} as valid for {@code @Pattern}) and fell through to the domain layer as a harmless
 * but inconsistent 422; now it is rejected at the boundary.
 */
class VerifyFirstAccessRequestTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  @Test
  void aFullyValidRequest_hasNoViolations() {
    VerifyFirstAccessRequest request =
        new VerifyFirstAccessRequest("52998224725", "001234567", LocalDate.parse("1988-03-12"));
    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void aNullCpf_isRejected() {
    VerifyFirstAccessRequest request =
        new VerifyFirstAccessRequest(null, "001234567", LocalDate.parse("1988-03-12"));
    Set<ConstraintViolation<VerifyFirstAccessRequest>> violations = validator.validate(request);
    assertThat(violations).isNotEmpty();
    assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("cpf");
  }

  @Test
  void aNullCardNumber_isRejected() {
    VerifyFirstAccessRequest request =
        new VerifyFirstAccessRequest("52998224725", null, LocalDate.parse("1988-03-12"));
    Set<ConstraintViolation<VerifyFirstAccessRequest>> violations = validator.validate(request);
    assertThat(violations).isNotEmpty();
    assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("cardNumber");
  }

  @Test
  void aBlankCpf_isRejected() {
    VerifyFirstAccessRequest request =
        new VerifyFirstAccessRequest("", "001234567", LocalDate.parse("1988-03-12"));
    assertThat(validator.validate(request)).isNotEmpty();
  }
}
