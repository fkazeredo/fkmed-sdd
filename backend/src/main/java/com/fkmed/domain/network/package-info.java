/**
 * The network module (SPEC-0008, Phase 3): accredited-provider network search.
 *
 * <p>Owns the geography registry {@code municipality} (IBGE code, name, {@code uf} FK → {@code
 * uf_registry}, seeded with the full official IBGE list — DL-0014), the registries {@code
 * service_type}, {@code specialty} and {@code seal} (reference data, baseline §0019), and {@code
 * provider} (+ {@code provider_specialty}/{@code provider_seal}) referencing a real municipality
 * plus a free-text {@code neighborhood} (Flyway V15). Read-only in this phase: {@link
 * com.fkmed.domain.network.NetworkSearch} is the public facade behind {@code
 * application.api.NetworkController} (funnel locality → service type → specialty → results, plus
 * name search), deriving offered localities from the **active** provider base filtered by the
 * plan's coverage (BR3/BR4, DL-0014). The {@code specialty} registry is a second, narrower public
 * facade ({@link com.fkmed.domain.network.NetworkSpecialties}) reused by {@code domain.appointment}
 * from SPEC-0009 onward (ADR-0011 Wave 2 freeze) — a one-directional appointment → network
 * dependency, no cycle. Module map: ADR-0011.
 */
@org.springframework.modulith.ApplicationModule(displayName = "network")
package com.fkmed.domain.network;
