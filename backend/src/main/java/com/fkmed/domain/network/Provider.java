package com.fkmed.domain.network;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

/**
 * An accredited provider (SPEC-0008): name, the {@link ServiceType} it offers, the {@link
 * Municipality} it operates in, a free-text {@code neighborhood} (BR3/DL-0014 — no authoritative
 * national neighborhood dataset), full address, phone and the {@code active} lifecycle flag that
 * BR13 enforces (an inactive provider never appears in lists or detail). Operator-loaded reference
 * mass, seeded by Flyway V15; read-only at runtime in this phase (ADR-0011).
 */
@Entity
@Table(name = "provider")
@Getter
public class Provider {

  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(name = "service_type_code", nullable = false)
  private String serviceTypeCode;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "municipality_id", nullable = false)
  private Municipality municipality;

  @Column(nullable = false)
  private String neighborhood;

  @Column private String cep;

  @Column private String street;

  @Column(name = "address_number")
  private String number;

  @Column private String complement;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private boolean active;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "provider_specialty", joinColumns = @JoinColumn(name = "provider_id"))
  @Column(name = "specialty_code")
  private Set<String> specialtyCodes;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "provider_seal", joinColumns = @JoinColumn(name = "provider_id"))
  @Column(name = "seal_code")
  private Set<String> sealCodes;

  /** JPA only. */
  protected Provider() {}

  /** Whether {@code value} matches this provider's neighborhood, exactly (BR7/BR9). */
  public boolean isInNeighborhood(String value) {
    return neighborhood.equalsIgnoreCase(value);
  }

  /** Whether {@code value} matches this provider's municipality name (BR1/BR8). */
  public boolean isInMunicipality(String value) {
    return municipality.getName().equalsIgnoreCase(value);
  }

  /** Whether {@code code} matches this provider's service type (BR5). */
  public boolean hasServiceType(String code) {
    return serviceTypeCode.equalsIgnoreCase(code);
  }

  /** Whether this provider carries {@code specialtyCode} (BR5/BR6), {@code null}-safe. */
  public boolean hasSpecialty(String specialtyCode) {
    return specialtyCode != null
        && specialtyCodes.stream().anyMatch(specialtyCode::equalsIgnoreCase);
  }

  /** Case/accent-insensitive substring match against the provider's name (BR8). */
  public boolean matchesName(String query) {
    return NormalizedText.contains(name, query);
  }

  /** The BR7 card locality label: {@code "BAIRRO, MUNICÍPIO – UF"}. */
  public String localityLabel() {
    return formatLocality(neighborhood, municipality.getName(), municipality.getUf());
  }

  /**
   * Pure formatting rule behind {@link #localityLabel()} (SPEC-0008 BR7/AC2: uppercase, "BAIRRO,
   * MUNICÍPIO – UF"), extracted as a static method for unit testing without a managed entity graph.
   */
  static String formatLocality(String neighborhood, String municipalityName, String uf) {
    return "%s, %s – %s"
        .formatted(
            neighborhood.toUpperCase(Locale.ROOT),
            municipalityName.toUpperCase(Locale.ROOT),
            uf.toUpperCase(Locale.ROOT));
  }
}
