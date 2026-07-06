/**
 * The telemedicine module: the 24/7 Pronto Atendimento virtual queue and scheduled teleconsultation
 * (SPEC-0010, ADR-0014 — the 11th Modulith module).
 *
 * <p>Owns the {@code symptom} and {@code tele_term} registries and {@code tele_session} with its
 * {@link com.fkmed.domain.telemedicine.TeleSessionState} state machine (BR11) guarded by an
 * optimistic {@code @Version} plus a single-active-session partial unique index (BR7), and the
 * queue/room lifecycle (Flyway V19). The beneficiary always sees the true session state, pushed
 * over SSE (ADR-0016/DL-0022) by the delivery layer.
 *
 * <p>Reuses the plan module's beneficiary scope ({@code telemedicine -> plan}, SPEC-0003) and the
 * appointment module's booking model for scheduled teleconsultation ({@code telemedicine ->
 * appointment}, DL-0018: a virtual Telemedicina unit + an additive modality flag + the join
 * window). Publishes {@link com.fkmed.domain.telemedicine.TeleTurnReached} and {@link
 * com.fkmed.domain.telemedicine.TeleSessionClosed} for the notification listener and the
 * closure&rarr;documents issuance to consume in Wave 2 (this module only publishes them). The
 * professional-side transitions (start attending, close) are driven by the SPEC-0018 operator
 * simulation (ADR-0017); {@link com.fkmed.domain.telemedicine.TeleService#reachTurn} and {@link
 * com.fkmed.domain.telemedicine.TeleService#close} are the seams it calls.
 */
@org.springframework.modulith.ApplicationModule(displayName = "telemedicine")
package com.fkmed.domain.telemedicine;
