package com.fkmed.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Production classes must satisfy every architecture rule (BOOTSTRAP §2 gate 4). The matching teeth
 * tests live in {@link ArchitectureTeethTest}.
 */
@AnalyzeClasses(packages = "com.fkmed", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domainDependsOnNothingAbove =
      ArchitectureRules.DOMAIN_DEPENDS_ON_NOTHING_ABOVE;

  @ArchTest
  static final ArchRule infraNeverDependsOnApplication =
      ArchitectureRules.INFRA_NEVER_DEPENDS_ON_APPLICATION;

  @ArchTest
  static final ArchRule moduleInternalIsRespected = ArchitectureRules.MODULE_INTERNAL_IS_RESPECTED;

  @ArchTest
  static final ArchRule entitiesHaveNoSetters = ArchitectureRules.ENTITIES_HAVE_NO_SETTERS;

  @ArchTest static final ArchRule noImplSuffix = ArchitectureRules.NO_IMPL_SUFFIX;

  @ArchTest static final ArchRule noFieldInjection = ArchitectureRules.NO_FIELD_INJECTION;

  @ArchTest static final ArchRule noSetterInjection = ArchitectureRules.NO_SETTER_INJECTION;

  @ArchTest
  static final ArchRule domainExceptionsDeclareCodeConstant =
      ArchitectureRules.DOMAIN_EXCEPTIONS_DECLARE_CODE_CONSTANT;
}
