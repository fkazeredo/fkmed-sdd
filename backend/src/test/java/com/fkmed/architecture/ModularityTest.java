package com.fkmed.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fkmed.FkmedApplication;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * BOOTSTRAP §2 gate 5: Spring Modulith boundary verification (acyclic, respected APIs) plus the
 * committed module-diagram snapshot with a drift gate. Regenerate the diagram with {@code
 * -Dmodulith.diagram.write=true}.
 *
 * <p>The snapshot is canonicalized before comparison ({@link #canonicalize}): the {@code Rel(...)}
 * relationship lines are sorted, because Spring Modulith's {@code Documenter} emits them in a
 * non-deterministic order across JVM runs (their order carries no architectural meaning). Every
 * relationship or component add/remove/change is still detected byte-for-byte — only the spurious
 * ordering drift is neutralized (SPEC-0004: surfaced by the notification module's mixed "depends
 * on"/"listens to" relationships).
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
            "domain.plan",
            "domain.error",
            "domain.identity",
            "domain.audit",
            "domain.content",
            "domain.card",
            "domain.notification",
            "domain.network",
            "domain.appointment",
            "domain.clinicaldocs",
            "domain.telemedicine",
            "domain.guides",
            "domain.finance");
  }

  @Test
  void moduleDiagram_matchesTheCommittedSnapshot() throws Exception {
    new Documenter(MODULES).writeModulesAsPlantUml();
    String generated =
        canonicalize(Files.readString(GENERATED, StandardCharsets.UTF_8).replace("\r\n", "\n"));

    if (Boolean.getBoolean("modulith.diagram.write")) {
      Files.createDirectories(SNAPSHOT.getParent());
      Files.writeString(SNAPSHOT, generated, StandardCharsets.UTF_8);
      return;
    }

    assertThat(SNAPSHOT)
        .as("missing committed module diagram — run ./mvnw verify -Dmodulith.diagram.write=true")
        .exists();
    String committed =
        canonicalize(Files.readString(SNAPSHOT, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    assertThat(generated)
        .as(
            "module diagram drift — review the module map (ADR-0001) and regenerate with "
                + "-Dmodulith.diagram.write=true")
        .isEqualTo(committed);
  }

  /**
   * Sorts the contiguous block(s) of {@code Rel(...)} lines so the non-deterministic order Spring
   * Modulith emits relationships in never causes spurious drift; all other lines keep their
   * position, so any structural change is still detected.
   */
  private static String canonicalize(String puml) {
    String[] lines = puml.split("\n", -1);
    List<String> out = new ArrayList<>(lines.length);
    int i = 0;
    while (i < lines.length) {
      if (lines[i].startsWith("Rel(")) {
        List<String> block = new ArrayList<>();
        while (i < lines.length && lines[i].startsWith("Rel(")) {
          block.add(lines[i]);
          i++;
        }
        Collections.sort(block);
        out.addAll(block);
      } else {
        out.add(lines[i]);
        i++;
      }
    }
    return String.join("\n", out);
  }
}
