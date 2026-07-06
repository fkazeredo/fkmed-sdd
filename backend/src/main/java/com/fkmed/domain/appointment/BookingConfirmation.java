package com.fkmed.domain.appointment;

/**
 * The confirmation returned by a booking, reschedule or cancellation (SPEC-0009 §I/O Examples): the
 * protocol and the resulting status (e.g. {@code {"protocol":"AG-20260705-0001",
 * "status":"AGENDADO"}}).
 */
public record BookingConfirmation(String protocol, AppointmentStatus status) {}
