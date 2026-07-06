package com.fkmed.application.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.guides.GuideType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0018 BR5 (fresh-eyes review, slice 5.1): a created guide item's {@code quantity} MUST be
 * positive. Before the fix it was an unconstrained {@code int}, so {@code POST /api/sim/guides}
 * accepted zero/negative quantities silently. The nested item record is named {@code
 * GuideItemRequest} (not {@code Item}) to avoid an OpenAPI schema-name collision with {@code
 * ClinicalDocumentListResponse.Item}.
 */
class SimCreateGuideRequestTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private static SimCreateGuideRequest requestWithQuantity(int quantity) {
    return new SimCreateGuideRequest(
        UUID.randomUUID(),
        GuideType.CONSULTA,
        "Clínica X",
        List.of(
            new SimCreateGuideRequest.GuideItemRequest("10101012", "Consulta médica", quantity)));
  }

  @Test
  void aPositiveQuantity_hasNoViolations() {
    assertThat(validator.validate(requestWithQuantity(1))).isEmpty();
  }

  @Test
  void aZeroQuantity_isRejected() {
    Set<ConstraintViolation<SimCreateGuideRequest>> violations =
        validator.validate(requestWithQuantity(0));
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("quantity"));
  }

  @Test
  void aNegativeQuantity_isRejected() {
    assertThat(validator.validate(requestWithQuantity(-3))).isNotEmpty();
  }
}
