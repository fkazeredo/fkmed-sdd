package com.fkmed.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.error.DomainException;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * BOOTSTRAP §2 gate 7: every {@code DomainException.CODE} (plus the generic handler code) has a
 * message in the product locale bundle ({@code messages.properties}, pt-BR — single-locale product,
 * so key parity is trivially the one bundle).
 */
class I18nCompletenessTest {

  @Test
  void everyDomainExceptionCode_hasAPtBrMessage() throws Exception {
    Properties bundle = new Properties();
    try (var stream = getClass().getResourceAsStream("/messages.properties")) {
      assertThat(stream).as("messages.properties must exist").isNotNull();
      bundle.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    List<JavaClass> exceptionTypes =
        new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.fkmed")
                .stream()
                .filter(c -> c.isAssignableTo(DomainException.class))
                .filter(c -> !c.getModifiers().contains(JavaModifier.ABSTRACT))
                .toList();
    assertThat(exceptionTypes).as("the scan must see the domain exceptions").isNotEmpty();

    for (JavaClass type : exceptionTypes) {
      String code = (String) Class.forName(type.getName()).getField("CODE").get(null);
      assertThat(bundle.stringPropertyNames())
          .as("missing pt-BR message for error code '%s' (%s)", code, type.getName())
          .contains(code);
      assertThat(bundle.getProperty(code)).isNotBlank();
    }

    assertThat(bundle.getProperty("internal.error"))
        .as("the generic 500 handler code must be in the bundle")
        .isNotBlank();
  }
}
