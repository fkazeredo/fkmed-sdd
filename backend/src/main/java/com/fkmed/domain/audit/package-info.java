/**
 * The audit module (SPEC-0003 foundation): the immutable, append-only audit trail and its recording
 * contract, consumed by every module that must leave an authorship record.
 *
 * <p>Owns the {@code audit_event} table (Flyway V4). Entries are written in the same transaction as
 * the action they record (BR4/BR6) and are never updated or record-level deleted (BR7); the only
 * sanctioned removal is the 12-month retention sweep (BR10). Event types are {@code *Codes}
 * constants, not an enum (baseline §0019). Module map: ADR-0001.
 */
@org.springframework.modulith.ApplicationModule(displayName = "audit")
package com.fkmed.domain.audit;
