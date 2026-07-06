/**
 * The appointment module: booking, cancelling and rescheduling consultations and exams in the
 * operator's own care units against real slot capacity (SPEC-0009, ADR-0012).
 *
 * <p>Owns {@code care_unit}, {@code unit_agenda}/{@code schedule_slot}, the {@code exam_type}
 * registry, {@code appointment} and {@code appointment_attachment} (Flyway V16). The first module
 * with real write concurrency: slot capacity is guarded by an optimistic {@code @Version} on {@link
 * com.fkmed.domain.appointment.ScheduleSlot} with fail-fast translation to {@link
 * com.fkmed.domain.appointment.SlotUnavailableException} (BR6, no retry loop — ADR-0012). Reuses
 * the plan module's beneficiary scope + protocol generator ({@code appointment -> plan}) and the
 * network module's specialty registry ({@code appointment -> network}); publishes {@code
 * AppointmentConfirmed}/{@code AppointmentCancelled}/{@code AppointmentRescheduled} for the
 * notification module to consume (wired at integration).
 */
@org.springframework.modulith.ApplicationModule(displayName = "appointment")
package com.fkmed.domain.appointment;
