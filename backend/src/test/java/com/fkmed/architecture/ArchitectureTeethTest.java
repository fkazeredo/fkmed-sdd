package com.fkmed.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Teeth tests (DECISIONS-BASELINE §0016): every ArchUnit rule must FAIL on its planted violation in
 * the {@code *.teeth} fixture packages — proving the rule actually bites. The fixtures live in test
 * sources only, so {@link ArchitectureTest} (production scope) never sees them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchitectureTeethTest {

  private JavaClasses fixtures;

  @BeforeAll
  void importFixtures() {
    fixtures =
        new ClassFileImporter()
            .importPackages(
                "com.fkmed.domain.teeth",
                "com.fkmed.application.teeth",
                "com.fkmed.infra.teeth",
                "com.fkmed.teeth",
                "fkmedteeth");
  }

  @Test
  void domainLayerRule_bites() {
    assertBites(ArchitectureRules.DOMAIN_DEPENDS_ON_NOTHING_ABOVE);
  }

  @Test
  void infraLayerRule_bites() {
    assertBites(ArchitectureRules.INFRA_NEVER_DEPENDS_ON_APPLICATION);
  }

  @Test
  void moduleInternalRule_bites() {
    assertBites(ArchitectureRules.MODULE_INTERNAL_IS_RESPECTED);
  }

  @Test
  void entitySetterRule_bites() {
    assertBites(ArchitectureRules.ENTITIES_HAVE_NO_SETTERS);
  }

  @Test
  void implSuffixRule_bites() {
    assertBites(ArchitectureRules.NO_IMPL_SUFFIX);
  }

  @Test
  void fieldInjectionRule_bites() {
    assertBites(ArchitectureRules.NO_FIELD_INJECTION);
  }

  @Test
  void setterInjectionRule_bites() {
    assertBites(ArchitectureRules.NO_SETTER_INJECTION);
  }

  @Test
  void domainExceptionCodeRule_bites() {
    assertBites(ArchitectureRules.DOMAIN_EXCEPTIONS_DECLARE_CODE_CONSTANT);
  }

  private void assertBites(ArchRule rule) {
    assertThat(rule.evaluate(fixtures).hasViolation())
        .as("rule [%s] must fail on its planted fixture", rule.getDescription())
        .isTrue();
  }
}
