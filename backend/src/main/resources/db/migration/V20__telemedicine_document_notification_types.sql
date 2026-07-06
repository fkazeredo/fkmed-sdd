-- SPEC-0010 × SPEC-0011 × SPEC-0004 (Phase 4 Wave 2): notification event types for the telemedicine
-- turn/closure and the clinical-document issuance, consumed by TeleNotificationListener and
-- ClinicalDocumentIssuedListener (wired at Wave 2). Same policy as the appointment business types
-- (email_default=true, mandatory=false — business, opt-outable): the beneficiary gets an in-app item
-- always, plus an e-mail unless they opted out.
insert into notification_event_type (code, description, email_default, mandatory) values
    ('tele.turn-reached', 'É a sua vez na Telemedicina', true, false),
    ('tele.session-closed', 'Atendimento de Telemedicina encerrado', true, false),
    ('clinical-document.issued', 'Novo documento em Minha Saúde', true, false);
