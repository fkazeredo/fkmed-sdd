package fkmedteeth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Planted violation: an entity exposing a setter, as @Data/@Setter would generate (teeth for
 * baseline §0013). Lives OUTSIDE {@code com.fkmed} so Hibernate's entity scan never sees it — the
 * setter rule has no package filter, so the teeth still bite.
 */
@Entity
public class SetterEntityFixture {

  @Id private Long id;

  public void setId(Long id) {
    this.id = id;
  }
}
