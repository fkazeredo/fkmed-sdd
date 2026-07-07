package com.fkmed.domain.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The updated value of an OVERDUE invoice (SPEC-0013 BR2, OQ1 owner-decided 2026-07-06): computed
 * on the ORIGINAL amount from the due date to today, deterministic, no external index.
 *
 * <ul>
 *   <li><b>multa</b> — fixed 2% of the original amount;
 *   <li><b>juros de mora</b> — 1% per month pro rata die: {@code original × 0.01 × diasEmAtraso ÷
 *       30};
 *   <li><b>total</b> — original + multa + juros.
 * </ul>
 *
 * All monetary results are rounded to cents (HALF_UP). The portal still offers NO online payment
 * (BR8): this value only informs; the guidance to the service channels stays.
 *
 * @param original the invoice's original amount.
 * @param fine the fixed 2% penalty.
 * @param interest the pro-rata-die 1%/month interest.
 * @param daysOverdue whole days between the due date and today.
 * @param total original + fine + interest.
 */
public record UpdatedAmount(
    BigDecimal original, BigDecimal fine, BigDecimal interest, long daysOverdue, BigDecimal total) {

  private static final BigDecimal FINE_RATE = new BigDecimal("0.02");
  private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.01");
  private static final BigDecimal DAYS_IN_MONTH = new BigDecimal("30");
  private static final int CENTS = 2;

  /**
   * Computes the updated amount for an original value {@code daysOverdue} days past due.
   *
   * @throws IllegalArgumentException when {@code daysOverdue} is not positive — the update is
   *     defined only for an OVERDUE invoice.
   */
  public static UpdatedAmount of(BigDecimal original, long daysOverdue) {
    if (daysOverdue <= 0) {
      throw new IllegalArgumentException("daysOverdue must be positive for an overdue invoice");
    }
    BigDecimal base = original.setScale(CENTS, RoundingMode.HALF_UP);
    BigDecimal fine = base.multiply(FINE_RATE).setScale(CENTS, RoundingMode.HALF_UP);
    BigDecimal interest =
        base.multiply(MONTHLY_INTEREST_RATE)
            .multiply(BigDecimal.valueOf(daysOverdue))
            .divide(DAYS_IN_MONTH, CENTS, RoundingMode.HALF_UP);
    BigDecimal total = base.add(fine).add(interest).setScale(CENTS, RoundingMode.HALF_UP);
    return new UpdatedAmount(base, fine, interest, daysOverdue, total);
  }
}
