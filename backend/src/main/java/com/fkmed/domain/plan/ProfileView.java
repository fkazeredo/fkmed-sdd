package com.fkmed.domain.plan;

import com.fkmed.domain.audit.Masking;
import java.time.LocalDate;

/**
 * The profile screen's read model (SPEC-0006): operator-owned contract data shown read-only (BR4 —
 * CPF masked), plus the beneficiary-owned editable contact/address data (BR5) and the avatar URL
 * (BR3, {@code null} when no photo is set so the client shows the placeholder).
 */
public record ProfileView(
    String fullName,
    String cpf,
    LocalDate birthDate,
    String cardNumber,
    String planName,
    String contactEmail,
    String mobile,
    String landline,
    String cep,
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String uf,
    String avatarUrl) {

  /** Builds the view from the beneficiary, masking the CPF and resolving the avatar URL. */
  static ProfileView from(Beneficiary beneficiary, String avatarUrl) {
    ContactInfo contact = beneficiary.getContact();
    return new ProfileView(
        beneficiary.getFullName(),
        Masking.cpf(beneficiary.getCpf()),
        beneficiary.getBirthDate(),
        beneficiary.getCardNumber(),
        beneficiary.getPlan().getName(),
        contact == null ? null : contact.getContactEmail(),
        contact == null ? null : contact.getMobile(),
        contact == null ? null : contact.getLandline(),
        contact == null ? null : contact.getCep(),
        contact == null ? null : contact.getStreet(),
        contact == null ? null : contact.getNumber(),
        contact == null ? null : contact.getComplement(),
        contact == null ? null : contact.getNeighborhood(),
        contact == null ? null : contact.getCity(),
        contact == null ? null : contact.getUf(),
        avatarUrl);
  }
}
