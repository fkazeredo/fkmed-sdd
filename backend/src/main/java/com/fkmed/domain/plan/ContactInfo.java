package com.fkmed.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Beneficiary-owned contact and address data (SPEC-0006 BR5/BR6), embedded in the {@code
 * beneficiary} row. A value object: instances are validated on creation and never mutated — a
 * partial update produces a new instance via {@link #apply}.
 *
 * <p>Invariants (§Validation Rules): the contact e-mail and mobile are mandatory and can never be
 * emptied (BR6); landline/CEP/UF are optional but format/registry-checked when present; address
 * text fields are free-form (length capped at the delivery boundary and by the column).
 */
@Embeddable
@Getter
public class ContactInfo {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern MOBILE = Pattern.compile("^\\(\\d{2}\\) \\d{5}-\\d{4}$");
  private static final Pattern LANDLINE = Pattern.compile("^\\(\\d{2}\\) \\d{4}-\\d{4}$");
  private static final Pattern CEP = Pattern.compile("^\\d{8}$");

  @Column(name = "contact_email")
  private String contactEmail;

  @Column private String mobile;

  @Column private String landline;

  @Column private String cep;

  @Column private String street;

  @Column(name = "address_number")
  private String number;

  @Column private String complement;

  @Column private String neighborhood;

  @Column private String city;

  @Column(length = 2)
  private String uf;

  /** JPA only. */
  protected ContactInfo() {}

  private ContactInfo(
      String contactEmail,
      String mobile,
      String landline,
      String cep,
      String street,
      String number,
      String complement,
      String neighborhood,
      String city,
      String uf) {
    this.contactEmail = contactEmail;
    this.mobile = mobile;
    this.landline = landline;
    this.cep = cep;
    this.street = street;
    this.number = number;
    this.complement = complement;
    this.neighborhood = neighborhood;
    this.city = city;
    this.uf = uf;
  }

  /** The empty contact (no data yet); a first update must supply the mandatory fields. */
  static ContactInfo empty() {
    return new ContactInfo();
  }

  /**
   * Applies a partial update onto this current contact and returns a new validated instance
   * (SPEC-0006 BR6/BR7). A {@code null} field keeps the current value; an empty string clears an
   * optional field or is rejected on a mandatory one.
   *
   * @throws ContactEmailRequiredException / {@link MobileRequiredException} when a mandatory field
   *     would end up empty.
   * @throws ContactEmailInvalidException / {@link MobileInvalidException} / {@link
   *     LandlineInvalidException} / {@link CepInvalidException} / {@link UfInvalidException} on a
   *     malformed value.
   */
  ContactInfo apply(ContactUpdate update, UfValidator ufValidator) {
    return new ContactInfo(
        requiredEmail(merge(update.contactEmail(), contactEmail)),
        requiredMobile(merge(update.mobile(), mobile)),
        optionalLandline(merge(update.landline(), landline)),
        optionalCep(merge(update.cep(), cep)),
        text(merge(update.street(), street)),
        text(merge(update.number(), number)),
        text(merge(update.complement(), complement)),
        text(merge(update.neighborhood(), neighborhood)),
        text(merge(update.city(), city)),
        optionalUf(merge(update.uf(), uf), ufValidator));
  }

  private static String merge(String updated, String current) {
    return updated == null ? current : updated;
  }

  private static String requiredEmail(String value) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      throw new ContactEmailRequiredException();
    }
    if (!EMAIL.matcher(normalized).matches()) {
      throw new ContactEmailInvalidException();
    }
    return normalized;
  }

  private static String requiredMobile(String value) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      throw new MobileRequiredException();
    }
    if (!MOBILE.matcher(normalized).matches()) {
      throw new MobileInvalidException();
    }
    return normalized;
  }

  private static String optionalLandline(String value) {
    String normalized = blankToNull(value);
    if (normalized != null && !LANDLINE.matcher(normalized).matches()) {
      throw new LandlineInvalidException();
    }
    return normalized;
  }

  private static String optionalCep(String value) {
    String normalized = blankToNull(value);
    if (normalized != null && !CEP.matcher(normalized).matches()) {
      throw new CepInvalidException();
    }
    return normalized;
  }

  private static String optionalUf(String value, UfValidator ufValidator) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      return null;
    }
    String upper = normalized.toUpperCase(Locale.ROOT);
    if (!ufValidator.isValid(upper)) {
      throw new UfInvalidException();
    }
    return upper;
  }

  private static String text(String value) {
    return blankToNull(value);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
