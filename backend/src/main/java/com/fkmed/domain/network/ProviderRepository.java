package com.fkmed.domain.network;

import com.fkmed.domain.ModuleInternal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence of {@link Provider}; internal to the network module (§0016). The {@code
 * findDistinctActive*} projections back the funnel's derived locality lists (BR3: only localities
 * with at least one active provider are offered); the {@code findByActiveTrue*} finders fetch full
 * entities for card/detail rendering, further filtered in {@link NetworkSearch} (accent-insensitive
 * name matching, BR2/BR8, is not expressible as a Spring Data derived query).
 */
@ModuleInternal
public interface ProviderRepository extends JpaRepository<Provider, UUID> {

  @Query("select distinct p.municipality.uf from Provider p where p.active = true")
  List<String> findDistinctActiveUfs();

  @Query(
      "select distinct p.municipality.name from Provider p "
          + "where p.active = true and upper(p.municipality.uf) = upper(:uf)")
  List<String> findDistinctActiveMunicipalityNames(@Param("uf") String uf);

  @Query(
      "select distinct p.neighborhood from Provider p where p.active = true "
          + "and upper(p.municipality.uf) = upper(:uf) "
          + "and upper(p.municipality.name) = upper(:municipality)")
  List<String> findDistinctActiveNeighborhoods(
      @Param("uf") String uf, @Param("municipality") String municipality);

  @Query(
      "select p from Provider p where p.active = true "
          + "and upper(p.municipality.uf) = upper(:uf) "
          + "and upper(p.municipality.name) = upper(:municipality)")
  List<Provider> findActiveByUfAndMunicipality(
      @Param("uf") String uf, @Param("municipality") String municipality);

  @Query("select p from Provider p where p.active = true and upper(p.municipality.uf) = upper(:uf)")
  List<Provider> findActiveByUf(@Param("uf") String uf);

  List<Provider> findByActiveTrue();
}
