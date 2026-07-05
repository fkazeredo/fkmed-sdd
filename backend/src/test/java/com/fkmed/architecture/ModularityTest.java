package com.fkmed.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.FkmedApplication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * BOOTSTRAP §2 gate 5: Spring Modulith boundary verification (acyclic, respected APIs) plus the
 * committed module-diagram snapshot with a drift gate. Regenerate the diagram with {@code
 * -Dmodulith.diagram.write=true}.
 */
class ModularityTest {

  private static final Path SNAPSHOT =
      Path.of("..", "docs", "architecture-diagrams", "modules.puml");
  private static final Path GENERATED =
      Path.of("target", "spring-modulith-docs", "components.puml");

  static final ApplicationModules MODULES = ApplicationModules.of(FkmedApplication.class);

  @Test
  void modules_respectBoundaries_andAreAcyclic() {
    MODULES.verify();
  }

  @Test
  void moduleMap_containsExactlyTheModulesOfAdr0001() {
    assertThat(MODULES.stream().map(m -> m.getIdentifier().toString()))
        .containsExactlyInAnyOrder(
            "domain.plan", "domain.error", "domain.identity", "domain.audit", "domain.content");
  }

  @Test
  void moduleDiagram_matchesTheCommittedSnapshot() throws Exception {
    new Documenter(MODULES).writeModulesAsPlantUml();
    String generated = Files.readString(GENERATED, StandardCharsets.UTF_8).replace("\r\n", "\n");

    if (Boolean.getBoolean("modulith.diagram.write")) {
      Files.createDirectories(SNAPSHOT.getParent());
      Files.writeString(SNAPSHOT, generated, StandardCharsets.UTF_8);
      return;
    }

    assertThat(SNAPSHOT)
        .as("missing committed module diagram — run ./mvnw verify -Dmodulith.diagram.write=true")
        .exists();
    String committed = Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertThat(generated)
        .as(
            "module diagram drift — review the module map (ADR-0001) and regenerate with "
                + "-Dmodulith.diagram.write=true")
        .isEqualTo(committed);
  }
}
