package com.fkmed.domain.finance;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The invoice write facade (SPEC-0013 §Business Context/BR8) — the ONLY way an invoice is created
 * or paid. The single caller is the operator simulation ({@code application.sim}, SPEC-0018); no
 * beneficiary write path exists (the portal is read-only over invoices). {@link #issue} publishes
 * {@link InvoiceIssued} inside the issuance transaction for the SPEC-0004 notification wiring;
 * {@link #pay} is idempotent (BR6). Seed-created invoices bypass this facade (a Flyway insert), so
 * they do NOT notify — only this sim path does.
 */
@Service
@RequiredArgsConstructor
public class Invoices {

  private final InvoiceRepository invoices;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  /**
   * Issues a new unpaid invoice for a titular and publishes {@link InvoiceIssued}. The command's
   * digitable line is normalized to its 47-digit canonical form first.
   *
   * @throws IllegalArgumentException when a required field is missing or the normalized line is not
   *     exactly 47 digits — an internal-contract violation by the calling sim.
   */
  @Transactional
  public InvoiceIssuedResult issue(IssueInvoiceCommand command) {
    Instant now = clock.instant();
    Invoice invoice =
        Invoice.issue(
            command.titularBeneficiaryId(),
            command.competencia(),
            command.dueDate(),
            command.amount(),
            DigitableLine.normalize(command.digitableLine()),
            command.pixCode(),
            now);
    invoices.save(invoice);
    String competencia = Competencia.label(invoice.getCompetencia());
    events.publishEvent(
        new InvoiceIssued(
            invoice.getId(),
            invoice.getTitularBeneficiaryId(),
            competencia,
            invoice.getAmount(),
            invoice.getDueDate()));
    return new InvoiceIssuedResult(invoice.getId(), competencia);
  }

  /**
   * Records the payment of an invoice idempotently (BR6): the first call stamps the payment
   * instant; a repeat on an already-paid invoice is a no-op. Publishes no event (payment is not a
   * notified fact in this slice).
   *
   * @return {@code true} when {@code invoiceId} exists (whether newly paid or already paid), {@code
   *     false} when there is no such invoice — the sim maps {@code false} to its stable {@code 404
   *     sim.target-not-found}.
   */
  @Transactional
  public boolean pay(UUID invoiceId) {
    return invoices
        .findById(invoiceId)
        .map(
            invoice -> {
              invoice.markPaid(clock.instant());
              return true;
            })
        .orElse(false);
  }
}
