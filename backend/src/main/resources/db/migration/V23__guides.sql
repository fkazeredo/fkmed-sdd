-- SPEC-0012 (Guides and Tokens), Phase 5: the domain.guides module (ADR-0018, 12th module).
-- Owns: guide (state machine EM_ANALISE -> AUTORIZADA | PARCIALMENTE_AUTORIZADA | NEGADA |
-- CANCELADA; AUTORIZADA/PARCIALMENTE_AUTORIZADA -> EXECUTADA | CANCELADA — BR6) guarded by an
-- optimistic version, its guide_item children (whose statuses the guide status derives from), and
-- attendance_token (BR9-BR12) guarded by a partial unique index mirroring V19's single-active-
-- session index. The guide.status-changed notification type was already seeded in V10 (unused
-- until this slice wires GuideStatusChangedListener). Fictitious POC reference mass, same
-- convention as V16/V19.

create table guide (
    id uuid primary key,
    number varchar(20) not null unique,
    type varchar(20) not null check (type in ('CONSULTA', 'SP_SADT', 'INTERNACAO')),
    beneficiary_id uuid not null,
    requesting_provider varchar(200) not null,
    requested_at date not null,
    status varchar(30) not null
        check (status in (
            'EM_ANALISE', 'AUTORIZADA', 'PARCIALMENTE_AUTORIZADA', 'NEGADA', 'CANCELADA', 'EXECUTADA'
        )),
    auth_password varchar(20),
    auth_valid_until date,
    denial_reason varchar(500),
    version bigint not null default 0
);

create index idx_guide_beneficiary on guide (beneficiary_id);

create table guide_item (
    id uuid primary key,
    guide_id uuid not null references guide (id),
    tuss_code varchar(20) not null,
    description varchar(200) not null,
    quantity integer not null,
    status varchar(20) not null check (status in ('EM_ANALISE', 'AUTORIZADO', 'NEGADO'))
);

create index idx_guide_item_guide on guide_item (guide_id);


-- Attendance token (BR9-BR12): a 6-digit code valid for 10 minutes, one non-invalidated token per
-- beneficiary at a time. created_by is the authoring account (self or a titular acting for a
-- dependent, BR12).
create table attendance_token (
    id uuid primary key,
    code varchar(6) not null,
    beneficiary_id uuid not null,
    generated_at timestamptz not null,
    expires_at timestamptz not null,
    invalidated_at timestamptz,
    created_by uuid
);

create index idx_attendance_token_beneficiary on attendance_token (beneficiary_id);

-- BR9: at most one non-invalidated token per beneficiary — generating a new one invalidates the
-- previous first (application-level), so the database never sees two live rows at once.
create unique index uq_attendance_token_active
    on attendance_token (beneficiary_id)
    where invalidated_at is null;


-- Canonical seed (SPEC-0012 §Persistence Changes): MARIA gets 3 guides with distinct statuses —
-- em análise (authorizable by the sim, AC7), autorizada (password AUT-482913, validity +30 days)
-- and negada ("Documentação insuficiente"); PEDRO gets none. Dates are relative to current_date so
-- a fresh Testcontainers database always seeds consistently regardless of when the suite runs.
insert into guide
    (id, number, type, beneficiary_id, requesting_provider, requested_at, status)
values
    (
        'ee100000-0000-4000-8000-000000000001',
        'GD-00000001',
        'CONSULTA',
        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
        'Dr. Ricardo Nunes - Cardiologia',
        current_date - 5,
        'EM_ANALISE'
    );

insert into guide
    (id, number, type, beneficiary_id, requesting_provider, requested_at, status,
     auth_password, auth_valid_until)
values
    (
        'ee100000-0000-4000-8000-000000000002',
        'GD-00000002',
        'SP_SADT',
        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
        'Laboratório Central',
        current_date - 20,
        'AUTORIZADA',
        'AUT-482913',
        current_date + 30
    );

insert into guide
    (id, number, type, beneficiary_id, requesting_provider, requested_at, status, denial_reason)
values
    (
        'ee100000-0000-4000-8000-000000000003',
        'GD-00000003',
        'INTERNACAO',
        '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
        'Hospital São Lucas',
        current_date - 35,
        'NEGADA',
        'Documentação insuficiente'
    );

insert into guide_item (id, guide_id, tuss_code, description, quantity, status) values
    (
        'ee200000-0000-4000-8000-000000000001',
        'ee100000-0000-4000-8000-000000000001',
        '10101012',
        'Consulta em consultório - cardiologia',
        1,
        'EM_ANALISE'
    ),
    (
        'ee200000-0000-4000-8000-000000000002',
        'ee100000-0000-4000-8000-000000000002',
        '40304361',
        'Hemograma completo',
        1,
        'AUTORIZADO'
    ),
    (
        'ee200000-0000-4000-8000-000000000003',
        'ee100000-0000-4000-8000-000000000003',
        '31009013',
        'Internação clínica - 3 diárias',
        3,
        'NEGADO'
    );
