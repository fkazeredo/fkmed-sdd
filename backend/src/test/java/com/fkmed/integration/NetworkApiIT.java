package com.fkmed.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * SPEC-0008 (Provider Network Search): funnel locality → service type → specialty → results, name
 * search and provider detail — all derived from the active provider base within the caller's plan
 * coverage (V15 seed: MARIA's plan is ESTADUAL/RJ). No fixture rows are inserted/mutated by this
 * class — every assertion reads the permanent V15 seed (same posture as {@code MigrationSeedIT}),
 * so no {@code @BeforeEach}/{@code @AfterEach} cleanup is needed (docs/architecture/testing.md).
 */
class NetworkApiIT extends AbstractIntegrationTest {

  private static final String MARIA_EMAIL = "maria@fkmed.local";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  // ---------------------------------------------------------------- authentication / coverage

  @Test
  void states_withoutAuthentication_returns401() throws Exception {
    mockMvc.perform(get("/api/network/states")).andExpect(status().isUnauthorized());
  }

  @Test
  void states_asMaria_returnsOnlyRj() throws Exception {
    mockMvc
        .perform(get("/api/network/states").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value("RJ"));
  }

  @Test
  void municipalities_ufOutsideCoverage_returns422OutsideCoverage() throws Exception {
    mockMvc
        .perform(get("/api/network/municipalities").param("uf", "SP").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("network.outside-coverage"));
  }

  // ---------------------------------------------------------------------------- derived locality
  // lists

  @Test
  void municipalities_filtersByQuery_caseAndAccentInsensitive_perSeed() throws Exception {
    // AC1: only municipalities with an active provider match "rio" in the RJ seed (Niterói has no
    // "rio" substring and is correctly excluded; the query is deliberately upper/without accents).
    mockMvc
        .perform(
            get("/api/network/municipalities")
                .param("uf", "RJ")
                .param("q", "RIO")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0]").value("Cabo Frio"))
        .andExpect(jsonPath("$[1]").value("Rio Bonito"))
        .andExpect(jsonPath("$[2]").value("Rio das Ostras"))
        .andExpect(jsonPath("$[3]").value("Rio de Janeiro"))
        .andExpect(jsonPath("$[4]").value("Três Rios"));
  }

  @Test
  void municipalities_withoutQuery_returnsEverySeededActiveMunicipality() throws Exception {
    mockMvc
        .perform(get("/api/network/municipalities").param("uf", "RJ").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(
            jsonPath("$")
                .value(
                    org.hamcrest.Matchers.containsInAnyOrder(
                        "Rio de Janeiro",
                        "Cabo Frio",
                        "Niterói",
                        "Três Rios",
                        "Rio Bonito",
                        "Rio das Ostras")));
  }

  @Test
  void neighborhoods_returnsRioDeJaneiroActiveNeighborhoods_sorted() throws Exception {
    mockMvc
        .perform(
            get("/api/network/neighborhoods")
                .param("uf", "RJ")
                .param("municipality", "Rio de Janeiro")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0]").value("Barra da Tijuca"))
        .andExpect(jsonPath("$[1]").value("Botafogo"))
        .andExpect(jsonPath("$[2]").value("Centro"))
        .andExpect(jsonPath("$[3]").value("Copacabana"))
        .andExpect(jsonPath("$[4]").value("Tijuca"));
  }

  // --------------------------------------------------------------------------------- registries

  @Test
  void serviceTypes_returnsTheBr5FixedList_onlyConsultoriosCarriesTheSpecialtyStep()
      throws Exception {
    mockMvc
        .perform(get("/api/network/service-types").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[0].code").value("CONSULTORIOS"))
        .andExpect(jsonPath("$[0].hasSpecialtyStep").value(true))
        .andExpect(jsonPath("$[1].hasSpecialtyStep").value(false))
        .andExpect(jsonPath("$[7].hasSpecialtyStep").value(false));
  }

  @Test
  void specialties_returnsAtLeast15_alphabetical() throws Exception {
    mockMvc
        .perform(get("/api/network/specialties").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(15)))
        .andExpect(jsonPath("$[0].name").value("Alergologia"));
  }

  // ----------------------------------------------------------------------------- funnel results

  @Test
  void providers_rioDeJaneiroCentroConsultoriosCardiologia_returnsAtLeast10_allCentroRj()
      throws Exception {
    // AC2.
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "RJ")
                .param("municipality", "Rio de Janeiro")
                .param("neighborhood", "Centro")
                .param("serviceType", "CONSULTORIOS")
                .param("specialty", "CARDIOLOGIA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.referenceDate").exists())
        .andExpect(jsonPath("$.items.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(10)))
        .andExpect(jsonPath("$.items.length()").value(11))
        .andExpect(jsonPath("$.items[0].locality").value("CENTRO, RIO DE JANEIRO – RJ"));
  }

  @Test
  void providers_excludesInactiveProviders_br13() throws Exception {
    // The seed also carries one INACTIVE Cardiologia/Centro/RJ provider; it must never surface.
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "RJ")
                .param("municipality", "Rio de Janeiro")
                .param("neighborhood", "Centro")
                .param("serviceType", "CONSULTORIOS")
                .param("specialty", "CARDIOLOGIA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.items[*].name")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("Clínica Cardiológica Desativada Centro"))));
  }

  @Test
  void providers_serviceTypeWithoutSpecialtyStep_skipsSpecialtyAndReturnsDirectly()
      throws Exception {
    // AC3: Laboratórios e Exames has no specialty step; results load with no specialty param.
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "RJ")
                .param("municipality", "Rio de Janeiro")
                .param("neighborhood", "Centro")
                .param("serviceType", "LABORATORIOS_EXAMES")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].name").value("Laboratório Central de Análises Clínicas"))
        .andExpect(jsonPath("$.items[0].serviceType").value("Laboratórios e Exames"));
  }

  @Test
  void providers_neighborhoodTodos_omitted_returnsWholeMunicipality() throws Exception {
    // BR9: no neighborhood filter -> every active Cardiologia/CONSULTORIOS provider in the
    // municipality (Centro + Copacabana + Tijuca), strictly more than the Centro-only count.
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "RJ")
                .param("municipality", "Rio de Janeiro")
                .param("serviceType", "CONSULTORIOS")
                .param("specialty", "CARDIOLOGIA")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(13));
  }

  @Test
  void providers_noMatches_returnsEmptyItemsNotAnError() throws Exception {
    // AC5's backend half: an empty result is a 200 with an empty list, never an error.
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "RJ")
                .param("municipality", "Três Rios")
                .param("neighborhood", "Bairro Inexistente")
                .param("serviceType", "CONSULTORIOS")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void providers_ufOutsideCoverage_returns422() throws Exception {
    mockMvc
        .perform(
            get("/api/network/providers")
                .param("uf", "SP")
                .param("municipality", "São Paulo")
                .param("serviceType", "CONSULTORIOS")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("network.outside-coverage"));
  }

  // -------------------------------------------------------------------------------- name search

  @Test
  void searchByName_lessThan3Chars_returns422QueryTooShort() throws Exception {
    mockMvc
        .perform(get("/api/network/providers/search").param("name", "ca").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("network.query-too-short"));
  }

  @Test
  void searchByName_cardio_findsActiveProvidersAcrossNeighborhoods_withinCoverage_ac8()
      throws Exception {
    // Name search matches the provider NAME text (not the specialty code): of the seed's 11
    // Centro/Cardiologia providers only those actually named "Cardio*"/"Cardiológic*" match, plus
    // one each in Copacabana, Tijuca, Cabo Frio, Niterói and Rio das Ostras -- 12 in total, never
    // the inactive "Clínica Cardiológica Desativada Centro".
    mockMvc
        .perform(
            get("/api/network/providers/search").param("name", "cardio").with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(12))
        .andExpect(
            jsonPath("$.items[*].name")
                .value(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("Clínica Cardiológica Desativada Centro"))))
        .andExpect(jsonPath("$.items[0].serviceType").exists());
  }

  @Test
  void searchByName_withMunicipalityFilter_narrowsResults() throws Exception {
    mockMvc
        .perform(
            get("/api/network/providers/search")
                .param("name", "cardio")
                .param("municipality", "Niterói")
                .with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].name").value("Clínica Cardiológica Niterói"));
  }

  // ------------------------------------------------------------------------------ provider detail

  @Test
  void providerDetail_returnsFullDetail_withAddressAndSeals() throws Exception {
    UUID id = providerIdByName("Hospital Regional de Niterói");
    mockMvc
        .perform(get("/api/network/providers/{id}", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Hospital Regional de Niterói"))
        .andExpect(jsonPath("$.serviceType").value("Hospitais para Internação"))
        .andExpect(jsonPath("$.address.municipality").value("Niterói"))
        .andExpect(jsonPath("$.address.uf").value("RJ"))
        .andExpect(jsonPath("$.address.neighborhood").value("Centro"))
        .andExpect(jsonPath("$.phone").value("(21) 2622-2002"))
        .andExpect(jsonPath("$.seals[0].code").value("ACR_HOSP"))
        .andExpect(jsonPath("$.seals[0].description").exists());
  }

  @Test
  void providerDetail_unknownId_returns410ProviderUnavailable() throws Exception {
    mockMvc
        .perform(get("/api/network/providers/{id}", UUID.randomUUID()).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.code").value("network.provider-unavailable"));
  }

  @Test
  void providerDetail_inactiveProvider_returns410ProviderUnavailable() throws Exception {
    // Matches the spec's literal example: an inactive provider's detail answers 410, identically
    // to an unknown id (BR13 — existence is never distinguishable).
    UUID id = providerIdByName("Hospital Desativado Niterói");
    mockMvc
        .perform(get("/api/network/providers/{id}", id).with(authAs(MARIA_EMAIL)))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.code").value("network.provider-unavailable"));
  }

  /**
   * Resolves a seeded provider's id directly from the V15 seed, without hardcoding its UUID (which
   * is randomly generated at migration-write time). Direct SQL rather than the search endpoint
   * because the inactive-provider scenario is by design invisible to every read endpoint except
   * detail (BR13).
   */
  private UUID providerIdByName(String providerName) {
    return jdbc.queryForObject("select id from provider where name = ?", UUID.class, providerName);
  }

  private static RequestPostProcessor authAs(String email) {
    return jwt().jwt(jwt -> jwt.subject(email));
  }
}
