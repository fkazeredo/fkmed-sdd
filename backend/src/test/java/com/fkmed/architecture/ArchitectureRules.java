package com.fkmed.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.fkmed.domain.ModuleInternal;
import com.fkmed.domain.error.DomainException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The ArchUnit rule set of BOOTSTRAP §2 gate 4 (DECISIONS-BASELINE §0010/§0012/§0013/§0016). Shared
 * between {@link ArchitectureTest} (production classes must pass) and {@link ArchitectureTeethTest}
 * (planted violations must fail — every rule has teeth).
 */
final class ArchitectureRules {

  private ArchitectureRules() {}

  /** Baseline §0012: the domain is the pure core — it imports nothing above it. */
  static final ArchRule DOMAIN_DEPENDS_ON_NOTHING_ABOVE =
      noClasses()
          .that()
          .resideInAPackage("com.fkmed.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.fkmed.application..", "com.fkmed.infra..")
          .as("domain must not depend on application or infra (baseline §0012)");

  /** Baseline §0010: driven adapters never know the delivery layer. */
  static final ArchRule INFRA_NEVER_DEPENDS_ON_APPLICATION =
      noClasses()
          .that()
          .resideInAPackage("com.fkmed.infra..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.fkmed.application..")
          .as("infra must not depend on application (baseline §0010)");

  /** Baseline §0016: @ModuleInternal types stay inside their module (infra exempt). */
  static final ArchRule MODULE_INTERNAL_IS_RESPECTED =
      classes()
          .that()
          .areAnnotatedWith(ModuleInternal.class)
          .should(onlyBeAccessedFromTheirOwnModuleOrInfra())
          .as("@ModuleInternal types are only accessed from their module or infra (§0016)");

  /** Baseline §0013: entities mutate through business methods only — no setters. */
  static final ArchRule ENTITIES_HAVE_NO_SETTERS =
      noMethods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(Entity.class)
          .should()
          .haveNameMatching("set[A-Z].*")
          .as("JPA entities must not expose setters — no @Data/@Setter (baseline §0013)");

  /** docs/architecture/backend.md §Code style: no *Impl naming. */
  static final ArchRule NO_IMPL_SUFFIX =
      noClasses()
          .should()
          .haveSimpleNameEndingWith("Impl")
          .as("no *Impl naming — interfaces are for real ports");

  /** docs/architecture/backend.md: constructor injection only. */
  static final ArchRule NO_FIELD_INJECTION =
      noFields()
          .should()
          .beAnnotatedWith(Autowired.class)
          .as("constructor injection only — no field @Autowired");

  /** docs/architecture/backend.md: constructor injection only (setter variant). */
  static final ArchRule NO_SETTER_INJECTION =
      noMethods()
          .should()
          .beAnnotatedWith(Autowired.class)
          .as("constructor injection only — no method @Autowired");

  /**
   * Supports the i18n completeness gate: every concrete DomainException declares its stable {@code
   * public static final String CODE} so the gate can enumerate all error codes.
   */
  static final ArchRule DOMAIN_EXCEPTIONS_DECLARE_CODE_CONSTANT =
      classes()
          .that()
          .areAssignableTo(DomainException.class)
          .and()
          .doNotHaveModifier(JavaModifier.ABSTRACT)
          .should(declareAPublicStaticFinalStringCode())
          .as("every concrete DomainException declares public static final String CODE");

  private static ArchCondition<JavaClass> onlyBeAccessedFromTheirOwnModuleOrInfra() {
    return new ArchCondition<>("only be accessed from their own module or from infra") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        javaClass
            .getAccessesToSelf()
            .forEach(
                access -> {
                  String originPackage = access.getOriginOwner().getPackageName();
                  boolean allowed =
                      originPackage.equals(javaClass.getPackageName())
                          || originPackage.startsWith("com.fkmed.infra");
                  if (!allowed) {
                    events.add(SimpleConditionEvent.violated(access, access.getDescription()));
                  }
                });
      }
    };
  }

  private static ArchCondition<JavaClass> declareAPublicStaticFinalStringCode() {
    return new ArchCondition<>("declare a public static final String CODE constant") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        boolean declared =
            javaClass.getFields().stream()
                .anyMatch(
                    field ->
                        field.getName().equals("CODE")
                            && field.getModifiers().contains(JavaModifier.PUBLIC)
                            && field.getModifiers().contains(JavaModifier.STATIC)
                            && field.getModifiers().contains(JavaModifier.FINAL)
                            && field.getRawType().isEquivalentTo(String.class));
        if (!declared) {
          events.add(
              SimpleConditionEvent.violated(
                  javaClass, javaClass.getName() + " declares no CODE constant"));
        }
      }
    };
  }
}
