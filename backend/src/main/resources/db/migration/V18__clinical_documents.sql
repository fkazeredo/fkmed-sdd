-- SPEC-0011 (Clinical Documents / Minha Saúde), Phase 4: the domain.clinicaldocs module
-- (ADR-0013, 10th verified module). clinical_document is the immutable header (type,
-- beneficiary, issuing professional + CRM, issue date, valid_until stamped at issue per
-- DL-0019, origin session/operator ref per BR8) plus the single-valued type fields (referral:
-- target_specialty_code/referral_reason; sick note: period/cid/notes — DL-0020, CID IS stored).
-- exam_order_item/prescription_item hold the list-valued fields (Rule Zero: two narrow child
-- tables rather than four near-empty type tables). No FK from origin_session_id/
-- origin_operator_id to other modules' tables (cross-module isolation — domain.telemedicine and
-- the operator-sim of SPEC-0018 are Wave 2, not built yet); no FK from target_specialty_code to
-- domain.network's specialty registry, mirroring appointment.specialty_code's precedent (V16).
--
-- Documents are created ONLY through the ClinicalDocuments.issue() facade — no update path exists
-- in the application (BR8 immutability); this migration seeds representative documents directly
-- because there is no other way to seed fixture data before that facade's callers exist.

create table clinical_document (
    id uuid primary key,
    type varchar(20) not null
        check (type in ('EXAM_ORDER', 'REFERRAL', 'PRESCRIPTION', 'SICK_NOTE')),
    beneficiary_id uuid not null,
    professional_name varchar(160) not null,
    crm varchar(30) not null,
    issued_at timestamptz not null,
    valid_until date,
    origin_session_id uuid,
    origin_operator_id uuid,
    clinical_indication varchar(500),
    target_specialty_code varchar(40),
    referral_reason varchar(500),
    sick_note_period_start date,
    sick_note_period_end date,
    cid varchar(10),
    sick_note_notes varchar(500),
    constraint chk_clinical_document_origin check (
        (origin_session_id is not null and origin_operator_id is null)
        or (origin_session_id is null and origin_operator_id is not null)
    )
);

create index idx_clinical_document_beneficiary on clinical_document (beneficiary_id);
create index idx_clinical_document_issued_at on clinical_document (issued_at);

create table exam_order_item (
    document_id uuid not null references clinical_document (id),
    item_order integer not null,
    exam_name varchar(120) not null,
    tuss_code varchar(20) not null,
    primary key (document_id, item_order)
);

create table prescription_item (
    document_id uuid not null references clinical_document (id),
    item_order integer not null,
    medication varchar(160) not null,
    posology varchar(200) not null,
    guidance varchar(500),
    primary key (document_id, item_order)
);


-- Representative seed (fictitious POC reference mass, same convention as V1): both types on both
-- MARIA (titular) and PEDRO (dependent), a mix of expired (BR5) and valid documents. Dates are
-- relative to the migration's current_date, so a fresh Testcontainers database always seeds a
-- deterministic before/after-expiry mix regardless of when the suite runs.

-- MARIA — PRESCRIPTION, issued 10 days ago, valid_until = issued + 30d (not expired).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_session_id)
values (
    'dd000000-0000-4000-8000-000000000001',
    'PRESCRIPTION',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'Dra. Camila Andrade',
    'CRM 55214 RJ',
    now() - interval '10 days',
    current_date - 10 + 30,
    'ee000000-0000-4000-8000-000000000001'
);
insert into prescription_item (document_id, item_order, medication, posology, guidance) values
    ('dd000000-0000-4000-8000-000000000001', 0, 'Amoxicilina 500mg', '1 comprimido a cada 8 horas por 7 dias', 'Tomar com alimentos'),
    ('dd000000-0000-4000-8000-000000000001', 1, 'Dipirona 500mg', '1 comprimido a cada 6 horas se dor', null);

-- MARIA — PRESCRIPTION, issued 40 days ago, valid_until = issued + 30d (EXPIRED — AC2/BR5).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_operator_id)
values (
    'dd000000-0000-4000-8000-000000000002',
    'PRESCRIPTION',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'Dr. Rafael Nunes',
    'CRM 48310 RJ',
    now() - interval '40 days',
    current_date - 40 + 30,
    'ee000000-0000-4000-8000-000000000002'
);
insert into prescription_item (document_id, item_order, medication, posology, guidance) values
    ('dd000000-0000-4000-8000-000000000002', 0, 'Loratadina 10mg', '1 comprimido ao dia por 10 dias', null);

-- MARIA — EXAM_ORDER, issued 5 days ago, valid_until = issued + 90d (not expired).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_session_id, clinical_indication)
values (
    'dd000000-0000-4000-8000-000000000003',
    'EXAM_ORDER',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'Dra. Camila Andrade',
    'CRM 55214 RJ',
    now() - interval '5 days',
    current_date - 5 + 90,
    'ee000000-0000-4000-8000-000000000001',
    'Investigação de fadiga e cefaleia recorrente'
);
insert into exam_order_item (document_id, item_order, exam_name, tuss_code) values
    ('dd000000-0000-4000-8000-000000000003', 0, 'Hemograma Completo', '40304361'),
    ('dd000000-0000-4000-8000-000000000003', 1, 'Ressonância Magnética de Crânio', '40901297');

-- MARIA — REFERRAL, issued 3 days ago, valid_until = issued + 90d (not expired) — AC4.
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_session_id, target_specialty_code, referral_reason)
values (
    'dd000000-0000-4000-8000-000000000004',
    'REFERRAL',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'Dra. Camila Andrade',
    'CRM 55214 RJ',
    now() - interval '3 days',
    current_date - 3 + 90,
    'ee000000-0000-4000-8000-000000000001',
    'CARDIOLOGIA',
    'Palpitações recorrentes — encaminhado para avaliação cardiológica'
);

-- MARIA — SICK_NOTE, issued 7 days ago, no validity (DL-0019) — CID displayed (DL-0020).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_operator_id, sick_note_period_start, sick_note_period_end, cid, sick_note_notes)
values (
    'dd000000-0000-4000-8000-000000000005',
    'SICK_NOTE',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'Dr. Rafael Nunes',
    'CRM 48310 RJ',
    now() - interval '7 days',
    null,
    'ee000000-0000-4000-8000-000000000002',
    current_date - 7,
    current_date - 5,
    'J11',
    'Repouso domiciliar recomendado'
);

-- PEDRO — PRESCRIPTION, issued 2 days ago, valid_until = issued + 30d (not expired).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_session_id)
values (
    'dd000000-0000-4000-8000-000000000006',
    'PRESCRIPTION',
    '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d',
    'Dra. Camila Andrade',
    'CRM 55214 RJ',
    now() - interval '2 days',
    current_date - 2 + 30,
    'ee000000-0000-4000-8000-000000000001'
);
insert into prescription_item (document_id, item_order, medication, posology, guidance) values
    ('dd000000-0000-4000-8000-000000000006', 0, 'Paracetamol 750mg', '1 comprimido a cada 6 horas se febre', null);

-- PEDRO — EXAM_ORDER, issued 100 days ago, valid_until = issued + 90d (EXPIRED — BR5).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_operator_id, clinical_indication)
values (
    'dd000000-0000-4000-8000-000000000007',
    'EXAM_ORDER',
    '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d',
    'Dr. Rafael Nunes',
    'CRM 48310 RJ',
    now() - interval '100 days',
    current_date - 100 + 90,
    'ee000000-0000-4000-8000-000000000002',
    'Check-up de rotina'
);
insert into exam_order_item (document_id, item_order, exam_name, tuss_code) values
    ('dd000000-0000-4000-8000-000000000007', 0, 'Hemograma Completo', '40304361');

-- PEDRO — REFERRAL, issued 95 days ago, valid_until = issued + 90d (EXPIRED — BR5).
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_session_id, target_specialty_code, referral_reason)
values (
    'dd000000-0000-4000-8000-000000000008',
    'REFERRAL',
    '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d',
    'Dra. Camila Andrade',
    'CRM 55214 RJ',
    now() - interval '95 days',
    current_date - 95 + 90,
    'ee000000-0000-4000-8000-000000000001',
    'DERMATOLOGIA',
    'Lesão de pele para avaliação dermatológica'
);

-- PEDRO — SICK_NOTE, issued 1 day ago, no validity.
insert into clinical_document
    (id, type, beneficiary_id, professional_name, crm, issued_at, valid_until,
     origin_operator_id, sick_note_period_start, sick_note_period_end, cid, sick_note_notes)
values (
    'dd000000-0000-4000-8000-000000000009',
    'SICK_NOTE',
    '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d',
    'Dr. Rafael Nunes',
    'CRM 48310 RJ',
    now() - interval '1 days',
    null,
    'ee000000-0000-4000-8000-000000000002',
    current_date - 1,
    current_date + 1,
    'M54.5',
    null
);
