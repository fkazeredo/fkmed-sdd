-- SPEC-0001: plan and beneficiary baseline + canonical seed (BR4/BR5).
-- Single-tenant per build (one medical provider per deployment) — the multi-tenant seam of
-- DECISIONS-BASELINE §0003 is intentionally dropped for FKMed (see ADR-0003).
-- Coverage stays a simple column; the registry table arrives with the spec that manages it
-- (baseline §0019 — see SPEC-0001 §Persistence Changes).

create table plan (
    id uuid primary key,
    name varchar(200) not null,
    ans_registration varchar(6) not null,
    coverage varchar(40) not null,
    copay boolean not null,
    reimbursement boolean not null,
    additives text[] not null default '{}'
);

create table beneficiary (
    id uuid primary key,
    plan_id uuid not null references plan (id),
    full_name varchar(200) not null,
    cpf varchar(11) not null,
    cns varchar(15) not null,
    card_number varchar(9) not null unique,
    birth_date date not null,
    role varchar(20) not null check (role in ('TITULAR', 'DEPENDENT')),
    titular_id uuid references beneficiary (id),
    active boolean not null default true,
    constraint chk_beneficiary_titular_link check (
        (role = 'TITULAR' and titular_id is null)
        or (role = 'DEPENDENT' and titular_id is not null)
    ),
    constraint chk_beneficiary_card_number check (card_number ~ '^[0-9]{9}$'),
    constraint chk_beneficiary_cns check (cns ~ '^[0-9]{15}$'),
    constraint chk_beneficiary_cpf check (cpf ~ '^[0-9]{11}$')
);

create index idx_beneficiary_plan on beneficiary (plan_id);
create index idx_beneficiary_titular on beneficiary (titular_id);

-- Canonical seed (SPEC-0001 BR4/BR5 + docs/specs/README.md §Canonical reference data).
-- Names, card numbers, CPFs and CNS are FICTITIOUS POC reference mass.
insert into plan (id, name, ans_registration, coverage, copay, reimbursement, additives)
values (
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
    'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
    '326305',
    'ESTADUAL',
    true,
    true,
    array['Urg/emerg Nacional Hr — Assistência']
);

insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active)
values
    (
        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
        'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
        'MARIA CLARA SOUZA LIMA',
        '52998224725',
        '700000000000001',
        '001234567',
        date '1988-03-12',
        'TITULAR',
        null,
        true
    ),
    (
        '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d',
        'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
        'PEDRO SOUZA LIMA',
        '15350946056',
        '700000000000002',
        '001234575',
        date '2007-05-20',
        'DEPENDENT',
        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
        true
    );
