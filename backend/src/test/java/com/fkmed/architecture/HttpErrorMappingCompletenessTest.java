package com.fkmed.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.domain.error.DomainException;
import com.fkmed.infra.web.HttpErrorMapping;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BOOTSTRAP §2 gate 8 (DECISIONS-BASELINE §0011): every concrete {@link DomainException} subclass
 * has an explicit HTTP status registered in {@link HttpErrorMapping} — the 422 fallback is a safety
 * net, never a steady state.
 */
class HttpErrorMappingCompletenessTest {

  @Test
  void everyDomainException_hasAnExplicitHttpMapping() {
    List<String> unmapped =
        new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.fkmed")
                .stream()
                .filter(c -> c.isAssignableTo(DomainException.class))
                .filter(c -> !c.getModifiers().contains(JavaModifier.ABSTRACT))
                .filter(
                    c ->
                        HttpErrorMapping.mappedTypes().stream()
                            .noneMatch(mapped -> mapped.getName().equals(c.getName())))
                .map(Object::toString)
                .toList();
    assertThat(unmapped)
        .as("DomainException subclasses without an explicit HttpErrorMapping entry")
        .isEmpty();
  }
}
