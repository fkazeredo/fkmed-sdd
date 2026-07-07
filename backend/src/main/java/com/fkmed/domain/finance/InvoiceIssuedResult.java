package com.fkmed.domain.finance;

import java.util.UUID;

/**
 * The outcome of issuing an invoice (SPEC-0013 §Operator-sim): the new invoice's id and its
 * "Mês/AAAA" competência (status is always OPEN at issue).
 */
public record InvoiceIssuedResult(UUID id, String competencia) {}
