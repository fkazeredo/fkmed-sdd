package com.fkmed.domain.reimbursement;

import java.time.LocalDate;
import java.util.UUID;

/** Small write-result contract shared by beneficiary actions and operator-sim actions. */
public record ReimbursementActionResult(
    UUID id, String protocol, ReimbursementStatus status, LocalDate expectedPaymentDate) {

  static ReimbursementActionResult of(ReimbursementRequest request) {
    return new ReimbursementActionResult(
        request.getId(),
        request.getProtocol(),
        request.getStatus(),
        request.getExpectedPaymentDate());
  }
}
