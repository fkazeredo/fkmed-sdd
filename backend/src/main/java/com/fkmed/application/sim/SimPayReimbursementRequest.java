package com.fkmed.application.sim;

/** Body of POST /api/sim/reimbursements/{id}/pay. */
public record SimPayReimbursementRequest(PaymentOutcome outcome, String failureReason) {

  public enum PaymentOutcome {
    SUCCESS,
    FAILURE
  }

  public boolean success() {
    return outcome == null || outcome == PaymentOutcome.SUCCESS;
  }
}
