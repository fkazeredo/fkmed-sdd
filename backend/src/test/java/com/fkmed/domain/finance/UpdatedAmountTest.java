package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * SPEC-0013 BR2 (OQ1): the overdue update — multa 2% fixed + juros de mora 1%/mês pro rata die — at
 * the boundaries the spec calls out (1 day, 30 days = 1 month, 45 days), plus rounding to cents.
 */
class UpdatedAmountTest {

  private static final BigDecimal ORIGINAL = new BigDecimal("489.90");

  @Test
  void oneDayOverdue_charges2percentFine_andOneThirtiethOfAmonthOfInterest() {
    UpdatedAmount updated = UpdatedAmount.of(ORIGINAL, 1);

    // multa = 489.90 * 0.02 = 9.798 -> 9.80
    assertThat(updated.fine()).isEqualByComparingTo("9.80");
    // juros = 489.90 * 0.01 * 1 / 30 = 0.16330 -> 0.16
    assertThat(updated.interest()).isEqualByComparingTo("0.16");
    assertThat(updated.total()).isEqualByComparingTo("499.86");
    assertThat(updated.original()).isEqualByComparingTo("489.90");
    assertThat(updated.daysOverdue()).isEqualTo(1);
  }

  @Test
  void thirtyDaysOverdue_isExactlyOneMonthOfInterest_onePercent() {
    UpdatedAmount updated = UpdatedAmount.of(ORIGINAL, 30);

    // juros = 489.90 * 0.01 * 30 / 30 = 4.899 -> 4.90 (== 1% of the amount)
    assertThat(updated.interest()).isEqualByComparingTo("4.90");
    assertThat(updated.fine()).isEqualByComparingTo("9.80");
    assertThat(updated.total()).isEqualByComparingTo("504.60");
  }

  @Test
  void fortyFiveDaysOverdue_isOneAndAHalfMonthsOfInterest() {
    UpdatedAmount updated = UpdatedAmount.of(ORIGINAL, 45);

    // juros = 489.90 * 0.01 * 45 / 30 = 7.34850 -> 7.35 (== 1.5% of the amount)
    assertThat(updated.interest()).isEqualByComparingTo("7.35");
    assertThat(updated.fine()).isEqualByComparingTo("9.80");
    assertThat(updated.total()).isEqualByComparingTo("507.05");
  }

  @Test
  void zeroOrNegativeDays_isRejected_theUpdateIsDefinedOnlyForAnOverdueInvoice() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> UpdatedAmount.of(ORIGINAL, 0));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> UpdatedAmount.of(ORIGINAL, -3));
  }

  @Test
  void resultsAreRoundedToCents_halfUp() {
    UpdatedAmount updated = UpdatedAmount.of(new BigDecimal("100.00"), 7);

    // juros = 100 * 0.01 * 7 / 30 = 0.23333 -> 0.23
    assertThat(updated.interest()).isEqualByComparingTo("0.23");
    assertThat(updated.fine()).isEqualByComparingTo("2.00");
    assertThat(updated.total()).isEqualByComparingTo("102.23");
    assertThat(updated.total().scale()).isEqualTo(2);
  }
}
