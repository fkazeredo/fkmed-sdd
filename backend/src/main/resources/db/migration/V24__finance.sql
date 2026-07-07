-- SPEC-0013 (Plano › Finanças), Phase 5: the domain.finance module. invoice holds the monthly
-- boletos issued to a contract's titular (digitable_line in its canonical digits-only 47-char form,
-- unique; amount is the ORIGINAL value on which the overdue update — multa 2% + juros 1%/mês pro
-- rata die, BR2/OQ1 — is computed at read time; status is DERIVED, never stored). copay_entry holds
-- the per-usage copay charges. IR statements and the Lei 12.007 settlement declaration are DERIVED
-- from the invoices (no table). No FK from titular_beneficiary_id/beneficiary_id to domain.plan's
-- beneficiary table (cross-module isolation — mirrors clinical_document.beneficiary_id, V18).
--
-- Invoices/copay entries are operator-originated (BR8): created only through the Invoices/Copays
-- facades (the operator sim, SPEC-0018). This migration seeds representative reference mass directly
-- (the same convention as V18) — seed invoices do NOT notify, only the sim path does.

create table invoice (
    id uuid primary key,
    titular_beneficiary_id uuid not null,
    competencia date not null,
    due_date date not null,
    amount numeric(12, 2) not null check (amount > 0),
    digitable_line varchar(47) not null unique check (digitable_line ~ '^[0-9]{47}$'),
    pix_code varchar(512) not null,
    paid_at timestamptz,
    created_at timestamptz not null
);

create index idx_invoice_titular on invoice (titular_beneficiary_id);

create table copay_entry (
    id uuid primary key,
    entry_date date not null,
    procedure varchar(160) not null,
    provider varchar(160) not null,
    beneficiary_id uuid not null,
    amount numeric(12, 2) not null check (amount > 0)
);

create index idx_copay_entry_beneficiary on copay_entry (beneficiary_id);
create index idx_copay_entry_date on copay_entry (entry_date);

-- SPEC-0013 × SPEC-0004: the new-invoice notification event type, consumed by InvoiceIssuedListener.
-- Business, opt-outable (email_default=true, mandatory=false), same policy as the other business
-- types: the titular gets an in-app item always, plus an e-mail unless they opted out.
insert into notification_event_type (code, description, email_default, mandatory) values
    ('finance.invoice-issued', 'Nova fatura disponível', true, false);

-- Representative seed for MARIA (titular, card 001234567 — id 3f2a1b4c…). Dates are relative to
-- current_date so a fresh Testcontainers database is always deterministic: the PRIOR calendar year
-- is FULLY PAID (2 paid invoices — offers the settlement declaration and has IR payments), the
-- CURRENT year is NOT settled (1 open current-month + 1 overdue). The overdue's due date is the last
-- day of the previous month (always strictly before today), so the overdue update is always shown.

-- MARIA — PAID, prior year, competência May.
insert into invoice
    (id, titular_beneficiary_id, competencia, due_date, amount, digitable_line, pix_code, paid_at, created_at)
values (
    'fa000000-0000-4000-8000-000000000001',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    make_date(extract(year from current_date)::int - 1, 5, 1),
    make_date(extract(year from current_date)::int - 1, 5, 10),
    452.75,
    '23793381286000826010494120780301189999000000001',
    '00020126580014br.gov.bcb.pix0136fkmed-2519-4a1b-8c2d-000000000001520400005303986540545 2.755802BR5909FKMED SAUDE6009SAO PAULO62070503***6304A1B2',
    make_date(extract(year from current_date)::int - 1, 5, 8) + time '10:00',
    make_date(extract(year from current_date)::int - 1, 4, 25) + time '08:00'
);

-- MARIA — PAID, prior year, competência September.
insert into invoice
    (id, titular_beneficiary_id, competencia, due_date, amount, digitable_line, pix_code, paid_at, created_at)
values (
    'fa000000-0000-4000-8000-000000000002',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    make_date(extract(year from current_date)::int - 1, 9, 1),
    make_date(extract(year from current_date)::int - 1, 9, 10),
    452.75,
    '23791762053000512038594876120345672000000000002',
    '00020126580014br.gov.bcb.pix0136fkmed-2519-4a1b-8c2d-000000000002520400005303986540545 2.755802BR5909FKMED SAUDE6009SAO PAULO62070503***6304C3D4',
    make_date(extract(year from current_date)::int - 1, 9, 8) + time '10:00',
    make_date(extract(year from current_date)::int - 1, 8, 25) + time '08:00'
);

-- MARIA — OPEN, current month (due 10 days from now).
insert into invoice
    (id, titular_beneficiary_id, competencia, due_date, amount, digitable_line, pix_code, paid_at, created_at)
values (
    'fa000000-0000-4000-8000-000000000003',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    date_trunc('month', current_date)::date,
    current_date + 10,
    489.90,
    '34191098765000432019874561230987650000000000003',
    '00020126580014br.gov.bcb.pix0136fkmed-2519-4a1b-8c2d-000000000003520400005303986540645 489.905802BR5909FKMED SAUDE6009SAO PAULO62070503***6304E5F6',
    null,
    current_date - 5 + time '09:00'
);

-- MARIA — OVERDUE, current month (due the last day of the previous month — always in the past).
insert into invoice
    (id, titular_beneficiary_id, competencia, due_date, amount, digitable_line, pix_code, paid_at, created_at)
values (
    'fa000000-0000-4000-8000-000000000004',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    date_trunc('month', current_date)::date,
    date_trunc('month', current_date)::date - 1,
    489.90,
    '00190500954014481606906809350888888000000000004',
    '00020126580014br.gov.bcb.pix0136fkmed-2519-4a1b-8c2d-000000000004520400005303986540645 489.905802BR5909FKMED SAUDE6009SAO PAULO62070503***6304G7H8',
    null,
    date_trunc('month', current_date)::date - 15 + time '09:00'
);

-- 8 copay charges within the last 90 days across MARIA (3f2a1b4c…) and PEDRO (9c8b7a6d…).
insert into copay_entry (id, entry_date, procedure, provider, beneficiary_id, amount) values
    ('fc000000-0000-4000-8000-000000000001', current_date - 5,  'Consulta — Clínica Geral',      'Clínica Vida Plena',        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 35.00),
    ('fc000000-0000-4000-8000-000000000002', current_date - 12, 'Consulta — Cardiologia',        'Instituto do Coração',      '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 45.00),
    ('fc000000-0000-4000-8000-000000000003', current_date - 20, 'Exame — Hemograma Completo',    'Laboratório Diagnose',      '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 18.50),
    ('fc000000-0000-4000-8000-000000000004', current_date - 33, 'Sessão — Fisioterapia',         'Clínica Movimento',         '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d', 25.00),
    ('fc000000-0000-4000-8000-000000000005', current_date - 47, 'Consulta — Pediatria',          'Clínica Infantil Sorriso',  '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d', 40.00),
    ('fc000000-0000-4000-8000-000000000006', current_date - 60, 'Exame — Ultrassonografia',      'Centro de Imagem Nitidez',  '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 55.00),
    ('fc000000-0000-4000-8000-000000000007', current_date - 75, 'Consulta — Dermatologia',       'Clínica Pele Saudável',     '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d', 45.00),
    ('fc000000-0000-4000-8000-000000000008', current_date - 88, 'Sessão — Psicologia',           'Espaço Bem-Estar',          '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 30.00);
