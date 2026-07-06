-- SPEC-0009 (Appointments), Phase 3: the domain.appointment module (ADR-0012) plus the shared
-- protocol generator owned by domain.plan (DL-0016, implementing SPEC-0003 BR9).
-- Owns: protocol_sequence (plan), care_unit, exam_type registry, unit_agenda/schedule_slot
-- (slot capacity guarded by an optimistic @Version — BR6) and appointment (unique protocol,
-- state machine AGENDADO -> REAGENDADO | CANCELADO; REALIZADO derived on read — BR11/BR12).
-- Fictitious POC reference mass, same convention as the V1 canonical seed.


-- Protocol generator (DL-0016), owned by domain.plan: a per-prefix, per-day atomic counter that
-- feeds the ^[A-Z]{2}-\d{8}-\d{4}$ protocol (SPEC-0003 BR9). SPEC-0009 is the first consumer (AG-).
create table protocol_sequence (
    prefix varchar(2) not null,
    seq_date date not null,
    counter integer not null,
    primary key (prefix, seq_date),
    constraint chk_protocol_prefix check (prefix ~ '^[A-Z]{2}$'),
    constraint chk_protocol_counter check (counter >= 1)
);


-- The operator's own care units (SPEC-0009 §Persistence: 2 seeded units with address).
create table care_unit (
    id uuid primary key,
    name varchar(140) not null,
    cep varchar(8),
    street varchar(160),
    address_number varchar(10),
    complement varchar(80),
    neighborhood varchar(100) not null,
    city varchar(100) not null,
    uf varchar(2) not null,
    phone varchar(20) not null,
    constraint chk_care_unit_cep check (cep is null or cep ~ '^\d{8}$')
);


-- Exam catalog (baseline §0019 registry): a String code validated by the module's registry,
-- label editable at runtime; wired branching, when any, goes through *Codes constants.
create table exam_type (
    code varchar(40) primary key,
    name varchar(80) not null
);

insert into exam_type (code, name) values
    ('HEMOGRAMA', 'Hemograma'),
    ('RAIO_X', 'Raio-X'),
    ('ULTRASSONOGRAFIA', 'Ultrassonografia'),
    ('RESSONANCIA_MAGNETICA', 'Ressonância Magnética'),
    ('TOMOGRAFIA', 'Tomografia'),
    ('MAMOGRAFIA', 'Mamografia'),
    ('ELETROCARDIOGRAMA', 'Eletrocardiograma'),
    ('ENDOSCOPIA', 'Endoscopia');


-- A (unit, scope) offering. scope_type CONSULTATION carries a specialty code (domain.network
-- registry), EXAM carries an exam_type code — no FK on scope_code because it spans two registries.
create table unit_agenda (
    id uuid primary key,
    unit_id uuid not null references care_unit (id),
    scope_type varchar(20) not null check (scope_type in ('CONSULTATION', 'EXAM')),
    scope_code varchar(40) not null,
    constraint uq_unit_agenda unique (unit_id, scope_type, scope_code)
);

create index idx_unit_agenda_scope on unit_agenda (scope_type, scope_code);


-- Bookable time slots with finite capacity; the occupied count is guarded by the version column
-- (JPA @Version) so a concurrent last-seat confirmation lets exactly one win (BR6/AC3).
create table schedule_slot (
    id uuid primary key,
    agenda_id uuid not null references unit_agenda (id),
    slot_date date not null,
    slot_time time not null,
    capacity integer not null,
    occupied integer not null default 0,
    version bigint not null default 0,
    constraint uq_schedule_slot unique (agenda_id, slot_date, slot_time),
    constraint chk_slot_capacity check (capacity > 0),
    constraint chk_slot_occupied check (occupied >= 0 and occupied <= capacity)
);

create index idx_schedule_slot_lookup on schedule_slot (agenda_id, slot_date, slot_time);


-- Appointment: unique protocol, state machine. Only the persistable states live in the check
-- constraint — REALIZADO is derived on read once the start instant passes (BR12). scheduled_at is
-- the real instant (UTC) of the slot, computed at the clinic timezone.
create table appointment (
    id uuid primary key,
    protocol varchar(20) not null unique,
    type varchar(20) not null check (type in ('CONSULTATION', 'EXAM')),
    beneficiary_id uuid not null,
    specialty_code varchar(40),
    exam_code varchar(40) references exam_type (code),
    unit_id uuid not null references care_unit (id),
    slot_id uuid not null references schedule_slot (id),
    scheduled_at timestamptz not null,
    status varchar(20) not null check (status in ('AGENDADO', 'REAGENDADO', 'CANCELADO')),
    cancel_reason varchar(200),
    created_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint chk_appointment_scope check (
        (type = 'CONSULTATION' and specialty_code is not null and exam_code is null)
        or (type = 'EXAM' and exam_code is not null and specialty_code is null)
    ),
    constraint chk_appointment_protocol check (protocol ~ '^[A-Z]{2}-\d{8}-\d{4}$')
);

create index idx_appointment_beneficiary on appointment (beneficiary_id);
create index idx_appointment_slot on appointment (slot_id);


-- Exam medical-order attachment (BR4): the content-validated bytes, one per exam appointment.
create table appointment_attachment (
    appointment_id uuid primary key references appointment (id),
    content bytea not null,
    content_type varchar(40) not null,
    file_name varchar(200),
    uploaded_at timestamptz not null
);


-- Seeded own units (fictitious POC reference mass).
insert into care_unit
    (id, name, cep, street, address_number, complement, neighborhood, city, uf, phone)
values
    (
        'aa000000-0000-4000-8000-000000000001',
        'FKMed Unidade Centro',
        '20040002',
        'Avenida Rio Branco',
        '1',
        null,
        'Centro',
        'Rio de Janeiro',
        'RJ',
        '(21) 3333-1000'
    ),
    (
        'aa000000-0000-4000-8000-000000000002',
        'FKMed Unidade Tijuca',
        '20510010',
        'Rua Conde de Bonfim',
        '500',
        null,
        'Tijuca',
        'Rio de Janeiro',
        'RJ',
        '(21) 3333-2000'
    );

-- Both units serve a representative set of specialties (domain.network registry) and exams.
insert into unit_agenda (id, unit_id, scope_type, scope_code)
select gen_random_uuid(), u.id, s.scope_type, s.scope_code
from care_unit u
cross join (values
    ('CONSULTATION', 'CARDIOLOGIA'),
    ('CONSULTATION', 'DERMATOLOGIA'),
    ('CONSULTATION', 'PEDIATRIA'),
    ('CONSULTATION', 'CLINICA_MEDICA'),
    ('CONSULTATION', 'GINECOLOGIA_OBSTETRICIA'),
    ('CONSULTATION', 'ORTOPEDIA_TRAUMATOLOGIA'),
    ('EXAM', 'HEMOGRAMA'),
    ('EXAM', 'RAIO_X'),
    ('EXAM', 'ULTRASSONOGRAFIA'),
    ('EXAM', 'TOMOGRAFIA')
) as s (scope_type, scope_code);

-- Seed the agenda for the next 30 days, Mon-Sat, 08:00-17:00 in 30-minute slots (last start
-- 16:30). Dates are relative to the migration's current_date, so a fresh Testcontainers database
-- always seeds "today -> +30 days" (SPEC-0009 §Persistence). Capacity 5 per slot.
insert into schedule_slot (id, agenda_id, slot_date, slot_time, capacity, occupied, version)
select gen_random_uuid(), a.id, d::date, t::time, 5, 0, 0
from unit_agenda a
cross join generate_series(
    current_date::timestamp, (current_date + 29)::timestamp, interval '1 day'
) as d
cross join generate_series(
    timestamp '2000-01-01 08:00', timestamp '2000-01-01 16:30', interval '30 minutes'
) as t
where extract(isodow from d) <= 6;
