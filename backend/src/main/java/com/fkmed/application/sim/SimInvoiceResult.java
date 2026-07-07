package com.fkmed.application.sim;

import java.util.UUID;

/**
 * The result of issuing an invoice via the sim (SPEC-0013 §Operator-sim): the new invoice's id, its
 * "Mês/AAAA" competência and its status (always {@code OPEN} at issue).
 */
public record SimInvoiceResult(UUID id, String competencia, String status) {}
