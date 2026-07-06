package com.fkmed.domain.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SPEC-0008 funnel + name search + detail — branch coverage for the mutation gate, with the
 * coverage boundary (BR4/DL-0014) as the security focus: every path is scoped to the caller's plan
 * coverage and denies by default when no plan resolves (fail-closed). Real {@link Provider}
 * fixtures exercise the entity's own filter methods; the repositories/coverage lookup are mocked.
 */
@ExtendWith(MockitoExtension.class)
class NetworkSearchTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);
  private static final String CARD = "001234567";

  @Mock private ProviderRepository providers;
  @Mock private ServiceTypeRepository serviceTypes;
  @Mock private SealRepository seals;
  @Mock private NetworkSpecialties specialties;
  @Mock private PlanCoverageLookup coverageLookup;

  private NetworkSearch search;

  @BeforeEach
  void setUp() {
    search = new NetworkSearch(providers, serviceTypes, seals, specialties, coverageLookup, CLOCK);
  }

  // ---- coverage boundary (BR4, fail-closed) ----------------------------------------------------

  @Test
  void states_returnsOnlyCoveredUfs_ofEstadualPlan() {
    covered("RJ");
    when(providers.findDistinctActiveUfs()).thenReturn(List.of("SP", "RJ", "MG"));

    assertThat(search.states(CARD)).containsExactly("RJ");
  }

  @Test
  void states_whenNoPlanResolves_deniesEveryUf_failClosed() {
    when(coverageLookup.coverageForCard(CARD)).thenReturn(Optional.empty());
    when(providers.findDistinctActiveUfs()).thenReturn(List.of("RJ", "SP"));

    assertThat(search.states(CARD)).isEmpty();
  }

  @Test
  void states_ofNacionalPlan_returnsEveryActiveUf_sorted() {
    when(coverageLookup.coverageForCard(CARD)).thenReturn(Optional.of(new PlanCoverage(null)));
    when(providers.findDistinctActiveUfs()).thenReturn(List.of("SP", "RJ"));

    assertThat(search.states(CARD)).containsExactly("RJ", "SP");
  }

  @Test
  void municipalities_outsideCoverage_isRejected() {
    covered("RJ");

    assertThatExceptionOfType(OutsideCoverageException.class)
        .isThrownBy(() -> search.municipalities(CARD, "SP", null));
  }

  @Test
  void municipalities_withinCoverage_filtersAccentInsensitively_andSorts() {
    covered("RJ");
    when(providers.findDistinctActiveMunicipalityNames("RJ"))
        .thenReturn(List.of("Rio de Janeiro", "Niterói", "Angra dos Reis"));

    assertThat(search.municipalities(CARD, "RJ", "niteroi")).containsExactly("Niterói");
    assertThat(search.municipalities(CARD, "RJ", null))
        .containsExactly("Angra dos Reis", "Niterói", "Rio de Janeiro");
  }

  @Test
  void neighborhoods_outsideCoverage_isRejected() {
    covered("RJ");

    assertThatExceptionOfType(OutsideCoverageException.class)
        .isThrownBy(() -> search.neighborhoods(CARD, "SP", "São Paulo"));
  }

  @Test
  void neighborhoods_withinCoverage_areSorted() {
    covered("RJ");
    when(providers.findDistinctActiveNeighborhoods("RJ", "Rio de Janeiro"))
        .thenReturn(List.of("Tijuca", "Centro"));

    assertThat(search.neighborhoods(CARD, "RJ", "Rio de Janeiro"))
        .containsExactly("Centro", "Tijuca");
  }

  // ---- registries ------------------------------------------------------------------------------

  @Test
  void serviceTypeOptions_carryTheSpecialtyStepFlag() {
    when(serviceTypes.findAllByOrderBySortOrderAsc())
        .thenReturn(
            List.of(
                serviceType("CONSULTORIOS", "Consultórios", true, 0),
                serviceType("EXAMES", "Exames", false, 1)));

    List<ServiceTypeOption> options = search.serviceTypeOptions();

    assertThat(options)
        .extracting(ServiceTypeOption::code)
        .containsExactly("CONSULTORIOS", "EXAMES");
    assertThat(options.get(0).hasSpecialtyStep()).isTrue();
    assertThat(options.get(1).hasSpecialtyStep()).isFalse();
  }

  @Test
  void specialtyOptions_delegatesToTheRegistryFacade() {
    when(specialties.all()).thenReturn(List.of(new SpecialtyOption("CARDIOLOGIA", "Cardiologia")));

    assertThat(search.specialtyOptions())
        .extracting(SpecialtyOption::code)
        .containsExactly("CARDIOLOGIA");
  }

  // ---- funnel ----------------------------------------------------------------------------------

  @Test
  void funnelResults_filterByNeighborhoodServiceTypeAndSpecialty_whenTheTypeCarriesTheStep() {
    covered("RJ");
    when(serviceTypes.findById("CONSULTORIOS"))
        .thenReturn(Optional.of(serviceType("CONSULTORIOS", "Consultórios", true, 0)));
    Provider match =
        provider(
            "Clínica A",
            "CONSULTORIOS",
            "Centro",
            "Rio de Janeiro",
            "RJ",
            Set.of("CARDIOLOGIA"),
            Set.of());
    Provider otherNeighborhood =
        provider(
            "Clínica B",
            "CONSULTORIOS",
            "Tijuca",
            "Rio de Janeiro",
            "RJ",
            Set.of("CARDIOLOGIA"),
            Set.of());
    Provider otherSpecialty =
        provider(
            "Clínica C",
            "CONSULTORIOS",
            "Centro",
            "Rio de Janeiro",
            "RJ",
            Set.of("DERMATOLOGIA"),
            Set.of());
    when(providers.findActiveByUfAndMunicipality("RJ", "Rio de Janeiro"))
        .thenReturn(List.of(otherSpecialty, match, otherNeighborhood));

    ProviderSearchResponse response =
        search.funnelResults(CARD, "RJ", "Rio de Janeiro", "Centro", "CONSULTORIOS", "CARDIOLOGIA");

    assertThat(response.referenceDate()).isEqualTo(LocalDate.of(2026, 7, 6));
    assertThat(response.items()).extracting(ProviderCard::name).containsExactly("Clínica A");
    assertThat(response.items().get(0).locality()).isEqualTo("CENTRO, RIO DE JANEIRO – RJ");
  }

  @Test
  void funnelResults_ignoreClientSpecialty_whenTheTypeHasNoSpecialtyStep() {
    covered("RJ");
    when(serviceTypes.findById("EXAMES"))
        .thenReturn(Optional.of(serviceType("EXAMES", "Exames", false, 1)));
    Provider a = provider("Lab X", "EXAMES", "Centro", "Rio de Janeiro", "RJ", Set.of(), Set.of());
    Provider b =
        provider(
            "Lab Y", "EXAMES", "Centro", "Rio de Janeiro", "RJ", Set.of("CARDIOLOGIA"), Set.of());
    when(providers.findActiveByUfAndMunicipality("RJ", "Rio de Janeiro")).thenReturn(List.of(a, b));

    // a client-sent specialty must be cleared server-side (BR5) → both exam labs match.
    ProviderSearchResponse response =
        search.funnelResults(CARD, "RJ", "Rio de Janeiro", null, "EXAMES", "CARDIOLOGIA");

    assertThat(response.items()).extracting(ProviderCard::name).containsExactly("Lab X", "Lab Y");
  }

  @Test
  void funnelResults_outsideCoverage_isRejected() {
    covered("RJ");

    assertThatExceptionOfType(OutsideCoverageException.class)
        .isThrownBy(
            () -> search.funnelResults(CARD, "SP", "São Paulo", null, "CONSULTORIOS", null));
  }

  // ---- name search -----------------------------------------------------------------------------

  @Test
  void searchByName_belowMinimumLength_isRejected() {
    assertThatExceptionOfType(NetworkQueryTooShortException.class)
        .isThrownBy(() -> search.searchByName(CARD, "ca", null));
  }

  @Test
  void searchByName_nacionalPlan_searchesEveryActiveProvider() {
    when(coverageLookup.coverageForCard(CARD)).thenReturn(Optional.of(new PlanCoverage(null)));
    Provider a =
        provider(
            "Cardio Centro", "CONSULTORIOS", "Centro", "Rio de Janeiro", "RJ", Set.of(), Set.of());
    Provider b =
        provider("Dermato Sul", "CONSULTORIOS", "Centro", "São Paulo", "SP", Set.of(), Set.of());
    when(providers.findByActiveTrue()).thenReturn(List.of(a, b));

    ProviderSearchResponse response = search.searchByName(CARD, "cardio", null);

    assertThat(response.items()).extracting(ProviderCard::name).containsExactly("Cardio Centro");
  }

  @Test
  void searchByName_estadualPlan_scopesToTheSingleCoveredUf_andMunicipality() {
    covered("RJ");
    Provider a =
        provider(
            "Cardio Rio", "CONSULTORIOS", "Centro", "Rio de Janeiro", "RJ", Set.of(), Set.of());
    Provider b =
        provider("Cardio Niteroi", "CONSULTORIOS", "Centro", "Niterói", "RJ", Set.of(), Set.of());
    when(providers.findActiveByUf("RJ")).thenReturn(List.of(a, b));

    ProviderSearchResponse response = search.searchByName(CARD, "cardio", "Rio de Janeiro");

    assertThat(response.items()).extracting(ProviderCard::name).containsExactly("Cardio Rio");
  }

  @Test
  void searchByName_whenNoPlanResolves_findsNothing_failClosed() {
    when(coverageLookup.coverageForCard(CARD)).thenReturn(Optional.empty());

    assertThat(search.searchByName(CARD, "cardio", null).items()).isEmpty();
  }

  // ---- detail (BR12/BR13) ----------------------------------------------------------------------

  @Test
  void detail_ofAnActiveProvider_rendersAddressSpecialtiesAndSeals() {
    UUID id = UUID.randomUUID();
    Provider provider =
        provider(
            "Clínica Cardio",
            "CONSULTORIOS",
            "Centro",
            "Rio de Janeiro",
            "RJ",
            Set.of("CARDIOLOGIA"),
            Set.of("ANS"));
    setField(provider, "id", id);
    when(providers.findById(id)).thenReturn(Optional.of(provider));
    when(serviceTypes.findById("CONSULTORIOS"))
        .thenReturn(Optional.of(serviceType("CONSULTORIOS", "Consultórios", true, 0)));
    when(specialties.namesOf(Set.of("CARDIOLOGIA"))).thenReturn(List.of("Cardiologia"));
    when(seals.findAllById(Set.of("ANS"))).thenReturn(List.of(seal("ANS", "Registro ANS", "desc")));

    ProviderDetailResponse response = search.detail(id);

    assertThat(response.serviceType()).isEqualTo("Consultórios");
    assertThat(response.specialties()).containsExactly("Cardiologia");
    assertThat(response.address().municipality()).isEqualTo("Rio de Janeiro");
    assertThat(response.seals()).extracting(SealBadge::name).containsExactly("Registro ANS");
  }

  @Test
  void detail_ofAnUnknownProvider_isUnavailable() {
    UUID id = UUID.randomUUID();
    when(providers.findById(id)).thenReturn(Optional.empty());

    assertThatExceptionOfType(ProviderUnavailableException.class)
        .isThrownBy(() -> search.detail(id));
  }

  @Test
  void detail_ofAnInactiveProvider_isUnavailable_neverRevealingIt() {
    UUID id = UUID.randomUUID();
    Provider inactive =
        provider("Antiga", "CONSULTORIOS", "Centro", "Rio de Janeiro", "RJ", Set.of(), Set.of());
    setField(inactive, "active", false);
    when(providers.findById(id)).thenReturn(Optional.of(inactive));

    assertThatExceptionOfType(ProviderUnavailableException.class)
        .isThrownBy(() -> search.detail(id));
  }

  // ---- fixtures --------------------------------------------------------------------------------

  private void covered(String uf) {
    lenient()
        .when(coverageLookup.coverageForCard(CARD))
        .thenReturn(Optional.of(new PlanCoverage(uf)));
  }

  private static Provider provider(
      String name,
      String serviceType,
      String neighborhood,
      String municipalityName,
      String uf,
      Set<String> specialtyCodes,
      Set<String> sealCodes) {
    Provider provider = new Provider();
    setField(provider, "id", UUID.randomUUID());
    setField(provider, "name", name);
    setField(provider, "serviceTypeCode", serviceType);
    setField(provider, "municipality", municipality(municipalityName, uf));
    setField(provider, "neighborhood", neighborhood);
    setField(provider, "cep", "20040002");
    setField(provider, "street", "Avenida Rio Branco");
    setField(provider, "number", "100");
    setField(provider, "phone", "(21) 2222-1000");
    setField(provider, "active", true);
    setField(provider, "specialtyCodes", specialtyCodes);
    setField(provider, "sealCodes", sealCodes);
    return provider;
  }

  private static Municipality municipality(String name, String uf) {
    Municipality municipality = new Municipality();
    setField(municipality, "ibgeCode", 3304557);
    setField(municipality, "name", name);
    setField(municipality, "uf", uf);
    return municipality;
  }

  private static ServiceType serviceType(String code, String name, boolean hasStep, int sortOrder) {
    ServiceType type = new ServiceType();
    setField(type, "code", code);
    setField(type, "name", name);
    setField(type, "hasSpecialtyStep", hasStep);
    setField(type, "sortOrder", sortOrder);
    return type;
  }

  private static Seal seal(String code, String name, String description) {
    Seal seal = new Seal();
    setField(seal, "code", code);
    setField(seal, "name", name);
    setField(seal, "description", description);
    return seal;
  }

  private static void setField(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
