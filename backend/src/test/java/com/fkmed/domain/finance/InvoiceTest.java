package com.fkmed.domain.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0013 BR2/BR6: derived status at the due-date boundary and the idempotent payment. */
class InvoiceTest {

  private static final UUID TITULAR = UUID.randomUUID();
  private static final LocalDate TODAY = LocalDate.of(2026, 7, 6);
  private static final String LINE = "23793381286000826010494120780301189999000000001";

  private static Invoice unpaidDue(LocalDate dueDate) {
    return Invoice.issue(
        TITULAR,
        LocalDate.of(2026, 7, 1),
        dueDate,
        new BigDecimal("489.90"),
        LINE,
        "pix",
        Instant.parse("2026-06-25T09:00:00Z"));
  }

  @Test
  void dueExactlyToday_isOpen_notOverdue() {
    assertThat(unpaidDue(TODAY).status(TODAY)).isEqualTo(InvoiceStatus.OPEN);
  }

  @Test
  void dueInTheFuture_isOpen() {
    assertThat(unpaidDue(TODAY.plusDays(10)).status(TODAY)).isEqualTo(InvoiceStatus.OPEN);
  }

  @Test
  void dueYesterday_isOverdue() {
    assertThat(unpaidDue(TODAY.minusDays(1)).status(TODAY)).isEqualTo(InvoiceStatus.OVERDUE);
  }

  @Test
  void paid_isPaid_regardlessOfDueDate() {
    Invoice invoice = unpaidDue(TODAY.minusDays(10));
    invoice.markPaid(Instant.parse("2026-07-01T12:00:00Z"));

    assertThat(invoice.status(TODAY)).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.paid()).isTrue();
  }

  @Test
  void markPaid_isIdempotent_keepsTheFirstPaymentInstant() {
    Invoice invoice = unpaidDue(TODAY.minusDays(10));
    Instant first = Instant.parse("2026-07-01T12:00:00Z");

    assertThat(invoice.markPaid(first)).isTrue();
    assertThat(invoice.markPaid(Instant.parse("2026-07-05T12:00:00Z"))).isFalse();
    assertThat(invoice.getPaidAt()).isEqualTo(first);
  }
}
