package com.fkmed.domain.network;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public facade of the network module (SPEC-0008): the funnel (state → municipality → neighborhood
 * → service type → specialty → results) and the name search, both restricted to the caller's plan
 * coverage (BR4, DL-0014) and to localities/providers derived from the **active** provider base
 * (BR3/BR13). The single entry point consumed by {@code application.api.NetworkController}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkSearch {

  private final ProviderRepository providers;
  private final ServiceTypeRepository serviceTypes;
  private final SealRepository seals;
  private final NetworkSpecialties specialties;
  private final PlanCoverageLookup coverageLookup;
  private final Clock clock;

  /** The UFs offering at least one active provider, within the caller's coverage (BR3/BR4). */
  public List<String> states(String beneficiaryCard) {
    PlanCoverage coverage = coverageFor(beneficiaryCard);
    return providers.findDistinctActiveUfs().stream()
        .filter(coverage::allowsUf)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  /**
   * Municipalities of {@code uf} offering at least one active provider, optionally filtered by a
   * case/accent-insensitive {@code query} (BR2/BR3).
   *
   * @throws OutsideCoverageException when {@code uf} is outside the caller's plan coverage.
   */
  public List<String> municipalities(String beneficiaryCard, String uf, String query) {
    requireCoverage(beneficiaryCard, uf);
    return providers.findDistinctActiveMunicipalityNames(uf).stream()
        .filter(name -> NormalizedText.contains(name, query))
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  /**
   * Neighborhoods of {@code municipality} (within {@code uf}) offering at least one active provider
   * (BR3/BR9).
   *
   * @throws OutsideCoverageException when {@code uf} is outside the caller's plan coverage.
   */
  public List<String> neighborhoods(String beneficiaryCard, String uf, String municipality) {
    requireCoverage(beneficiaryCard, uf);
    return providers.findDistinctActiveNeighborhoods(uf, municipality).stream()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  /** The service-type registry catalog, in its fixed BR5 display order. */
  public List<ServiceTypeOption> serviceTypeOptions() {
    return serviceTypes.findAllByOrderBySortOrderAsc().stream()
        .map(t -> new ServiceTypeOption(t.getCode(), t.getName(), t.specialtyStepApplies()))
        .toList();
  }

  /** The specialty registry catalog, alphabetical (BR6). */
  public List<SpecialtyOption> specialtyOptions() {
    return specialties.all();
  }

  /**
   * Funnel results (BR7): active providers of {@code serviceType} in {@code municipality}
   * (optionally narrowed to {@code neighborhood}; blank/absent means the whole municipality, BR9),
   * further narrowed by {@code specialty} only when the service type carries the specialty step
   * (BR5 enforced server-side — {@link ServiceType#clearSpecialtyOutsideItsStep}).
   *
   * @throws OutsideCoverageException when {@code uf} is outside the caller's plan coverage.
   */
  public ProviderSearchResponse funnelResults(
      String beneficiaryCard,
      String uf,
      String municipality,
      String neighborhood,
      String serviceTypeCode,
      String specialtyCode) {
    requireCoverage(beneficiaryCard, uf);
    String effectiveSpecialty = effectiveSpecialtyFor(serviceTypeCode, specialtyCode);

    List<Provider> matches =
        providers.findActiveByUfAndMunicipality(uf, municipality).stream()
            .filter(
                p ->
                    neighborhood == null
                        || neighborhood.isBlank()
                        || p.isInNeighborhood(neighborhood))
            .filter(p -> p.hasServiceType(serviceTypeCode))
            .filter(p -> effectiveSpecialty == null || p.hasSpecialty(effectiveSpecialty))
            .sorted(Comparator.comparing(Provider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    return toResponse(matches);
  }

  /**
   * Name search (BR8): active providers within the caller's plan coverage whose name contains
   * {@code name} (case/accent-insensitive, trimmed), optionally narrowed to {@code municipality}.
   *
   * @throws NetworkQueryTooShortException when the trimmed query has fewer than {@value
   *     NetworkQueryTooShortException#MIN_LENGTH} characters.
   */
  public ProviderSearchResponse searchByName(
      String beneficiaryCard, String name, String municipality) {
    String trimmed = name == null ? "" : name.strip();
    if (trimmed.length() < NetworkQueryTooShortException.MIN_LENGTH) {
      throw new NetworkQueryTooShortException();
    }
    PlanCoverage coverage = coverageFor(beneficiaryCard);
    List<Provider> pool =
        coverage.allowsEveryUf()
            ? providers.findByActiveTrue()
            : coverage.singleUf().map(providers::findActiveByUf).orElseGet(List::of);

    List<Provider> matches =
        pool.stream()
            .filter(
                p ->
                    municipality == null
                        || municipality.isBlank()
                        || p.isInMunicipality(municipality))
            .filter(p -> p.matchesName(trimmed))
            .sorted(Comparator.comparing(Provider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    return toResponse(matches);
  }

  /**
   * A provider's detail (BR12).
   *
   * @throws ProviderUnavailableException when the id is unknown or the provider is inactive (BR13).
   */
  public ProviderDetailResponse detail(UUID providerId) {
    Provider provider =
        providers
            .findById(providerId)
            .filter(Provider::isActive)
            .orElseThrow(ProviderUnavailableException::new);
    return new ProviderDetailResponse(
        provider.getId(),
        provider.getName(),
        nameOfServiceType(provider.getServiceTypeCode()),
        specialties.namesOf(provider.getSpecialtyCodes()),
        new AddressView(
            provider.getCep(),
            provider.getStreet(),
            provider.getNumber(),
            provider.getComplement(),
            provider.getNeighborhood(),
            provider.getMunicipality().getName(),
            provider.getMunicipality().getUf()),
        provider.getPhone(),
        sealBadgesOf(provider.getSealCodes()));
  }

  /** BR5: clears a client-sent specialty when {@code serviceTypeCode} carries no specialty step. */
  private String effectiveSpecialtyFor(String serviceTypeCode, String specialtyCode) {
    String normalizedCode = serviceTypeCode == null ? "" : serviceTypeCode.toUpperCase(Locale.ROOT);
    boolean specialtyStepApplies =
        serviceTypes.findById(normalizedCode).map(ServiceType::specialtyStepApplies).orElse(false);
    return ServiceType.clearSpecialtyOutsideItsStep(specialtyStepApplies, specialtyCode);
  }

  private ProviderSearchResponse toResponse(List<Provider> matches) {
    List<ProviderCard> cards =
        matches.stream()
            .map(
                p ->
                    new ProviderCard(
                        p.getId(),
                        p.getName(),
                        p.localityLabel(),
                        nameOfServiceType(p.getServiceTypeCode()),
                        sealBadgesOf(p.getSealCodes())))
            .toList();
    return new ProviderSearchResponse(LocalDate.now(clock), cards);
  }

  private PlanCoverage coverageFor(String beneficiaryCard) {
    return coverageLookup.coverageForCard(beneficiaryCard).orElse(PlanCoverage.NONE);
  }

  private void requireCoverage(String beneficiaryCard, String uf) {
    if (!coverageFor(beneficiaryCard).allowsUf(uf)) {
      throw new OutsideCoverageException();
    }
  }

  private String nameOfServiceType(String code) {
    return serviceTypes.findById(code).map(ServiceType::getName).orElse(code);
  }

  private List<SealBadge> sealBadgesOf(Set<String> codes) {
    if (codes.isEmpty()) {
      return List.of();
    }
    return seals.findAllById(codes).stream()
        .map(s -> new SealBadge(s.getCode(), s.getName(), s.getDescription()))
        .sorted(Comparator.comparing(SealBadge::code))
        .toList();
  }
}
