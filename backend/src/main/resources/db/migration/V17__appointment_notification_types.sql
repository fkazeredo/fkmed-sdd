-- SPEC-0009 × SPEC-0004: notification event types for the appointment lifecycle. `appointment.confirmed`
-- was already seeded in V10; this adds the cancel and reschedule types with the same policy
-- (email_default=true, mandatory=false — business, opt-outable), consumed by the
-- AppointmentNotificationListener wired at integration.
insert into notification_event_type (code, description, email_default, mandatory) values
    ('appointment.cancelled', 'Agendamento cancelado', true, false),
    ('appointment.rescheduled', 'Agendamento reagendado', true, false);
