package com.fkmed.application.api;

import com.fkmed.domain.network.NetworkSearch;
import com.fkmed.domain.network.ProviderDetailResponse;
import com.fkmed.domain.network.ProviderSearchResponse;
import com.fkmed.domain.network.ServiceTypeOption;
import com.fkmed.domain.network.SpecialtyOption;
import com.fkmed.infra.security.UserContextProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The provider network search endpoints (SPEC-0008): funnel locality → service type → specialty →
 * results, plus name search and provider detail. Read-only; every list is derived from the active
 * provider base within the caller's plan coverage (BR3/BR4).
 */
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

  private final NetworkSearch networkSearch;
  private final UserContextProvider userContext;

  /** UFs offering an active provider, within the caller's plan coverage (BR3/BR4). */
  @GetMapping("/states")
  List<String> states() {
    return networkSearch.states(card());
  }

  /** Municipalities of {@code uf}, optionally filtered by {@code q} (BR2/BR3). */
  @GetMapping("/municipalities")
  List<String> municipalities(@RequestParam String uf, @RequestParam(required = false) String q) {
    return networkSearch.municipalities(card(), uf, q);
  }

  /** Neighborhoods of {@code municipality} within {@code uf} (BR3/BR9). */
  @GetMapping("/neighborhoods")
  List<String> neighborhoods(@RequestParam String uf, @RequestParam String municipality) {
    return networkSearch.neighborhoods(card(), uf, municipality);
  }

  /** The fixed service-type registry (BR5). */
  @GetMapping("/service-types")
  List<ServiceTypeOption> serviceTypes() {
    return networkSearch.serviceTypeOptions();
  }

  /** The specialty registry, alphabetical (BR6). */
  @GetMapping("/specialties")
  List<SpecialtyOption> specialties() {
    return networkSearch.specialtyOptions();
  }

  /** Funnel results (BR7): exact filters, active providers only, today's reference date. */
  @GetMapping("/providers")
  ProviderSearchResponse providers(
      @RequestParam String uf,
      @RequestParam String municipality,
      @RequestParam(required = false) String neighborhood,
      @RequestParam String serviceType,
      @RequestParam(required = false) String specialty) {
    return networkSearch.funnelResults(
        card(), uf, municipality, neighborhood, serviceType, specialty);
  }

  /** Name search (BR8): active providers within coverage matching {@code name} (>= 3 chars). */
  @GetMapping("/providers/search")
  ProviderSearchResponse searchProviders(
      @RequestParam String name, @RequestParam(required = false) String municipality) {
    return networkSearch.searchByName(card(), name, municipality);
  }

  /** Provider detail (BR12); 410 when unknown or inactive (BR13). */
  @GetMapping("/providers/{id}")
  ProviderDetailResponse provider(@PathVariable UUID id) {
    return networkSearch.detail(id);
  }

  private String card() {
    return userContext.current().beneficiaryCard().orElse(null);
  }
}
