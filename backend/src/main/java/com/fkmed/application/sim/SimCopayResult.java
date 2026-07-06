package com.fkmed.application.sim;

import java.util.UUID;

/**
 * The result of recording a copay charge via the sim (SPEC-0013 §Operator-sim): the new entry's id.
 */
public record SimCopayResult(UUID id) {}
