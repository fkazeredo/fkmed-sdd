-- SPEC-0010 (Telemedicine), Phase 4: the domain.telemedicine module (ADR-0014, 11th module).
-- Owns: symptom + tele_term registries, tele_session (state machine EM_FILA ->
-- EM_ATENDIMENTO | ABANDONADA; EM_ATENDIMENTO -> ENCERRADA; finals ENCERRADA/ABANDONADA — BR11)
-- guarded by an optimistic @Version, its selected-symptom collection, and the single-active-session
-- guarantee (BR7) enforced by a partial unique index. Scheduled teleconsultation reuses
-- domain.appointment (DL-0018): an additive Telemedicina modality on appointment plus a seeded
-- virtual "Telemedicina" care unit whose agenda/slots back the tele agenda (30-day horizon), marked
-- by an additive virtual flag on care_unit. Fictitious POC reference mass, same convention as V16.


-- Symptom catalog (baseline §0019 registry): a String code validated by the module's registry on
-- every triage, label editable at runtime (SPEC-0010 BR2, §Persistence).
create table symptom (
    code varchar(40) primary key,
    name varchar(80) not null
);

insert into symptom (code, name) values
    ('CEFALEIA', 'Dor de cabeça'),
    ('FEBRE', 'Febre'),
    ('TOSSE', 'Tosse'),
    ('DOR_GARGANTA', 'Dor de garganta'),
    ('CORIZA', 'Coriza / nariz entupido'),
    ('NAUSEA', 'Náusea ou vômito'),
    ('DIARREIA', 'Diarreia'),
    ('DOR_ABDOMINAL', 'Dor abdominal'),
    ('DOR_MUSCULAR', 'Dor muscular'),
    ('FALTA_AR', 'Falta de ar'),
    ('DOR_TORACICA', 'Dor no peito'),
    ('TONTURA', 'Tontura'),
    ('ERUPCAO_PELE', 'Manchas ou erupções na pele'),
    ('FADIGA', 'Cansaço / fadiga');


-- Versioned teleattendance term (SPEC-0010 BR4, §Persistence): the current term is the most recent
-- published version; entering the queue requires accepting exactly that version.
create table tele_term (
    version varchar(20) primary key,
    published_at timestamptz not null,
    body text not null
);

insert into tele_term (version, published_at, body) values
    (
        '1.0',
        timestamptz '2026-01-01 00:00:00-03',
        'Termo de Teleatendimento FKMed. Ao entrar na fila do Pronto Atendimento, você concorda com a '
        || 'realização de atendimento médico a distância. O atendimento não substitui situações de '
        || 'urgência ou emergência com risco de vida, que devem ser direcionadas ao pronto-socorro mais '
        || 'próximo. Os dados clínicos informados são confidenciais e utilizados exclusivamente para o '
        || 'seu cuidado.'
    );


-- Telemedicine session (SPEC-0010 §Persistence, BR11 state machine). state guarded by the version
-- column (JPA @Version): concurrent transitions and the single-session race let exactly one win.
-- Only WALK_IN sessions carry triage (complaint/symptoms/duration); SCHEDULED sessions bridge from
-- a domain.appointment booking (appointment_id) opened through the join window (BR14).
create table tele_session (
    id uuid primary key,
    beneficiary_id uuid not null,
    type varchar(20) not null check (type in ('WALK_IN', 'SCHEDULED')),
    state varchar(20) not null
        check (state in ('EM_FILA', 'EM_ATENDIMENTO', 'ENCERRADA', 'ABANDONADA')),
    complaint varchar(500),
    other_symptom varchar(200),
    duration_code varchar(20),
    professional_name varchar(140),
    professional_crm varchar(30),
    guidance varchar(1000),
    term_version varchar(20),
    appointment_id uuid,
    queue_entered_at timestamptz,
    called_at timestamptz,
    started_at timestamptz,
    ended_at timestamptz,
    created_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint chk_tele_complaint_length
        check (complaint is null or char_length(complaint) between 10 and 500)
);

create index idx_tele_session_beneficiary on tele_session (beneficiary_id);
create index idx_tele_session_queue on tele_session (type, state, queue_entered_at);

-- BR7: at most one active WALK_IN queue session per beneficiary (a second Pronto Atendimento
-- resumes the existing one). Enforced at the database so the concurrent double-POST race cannot
-- create two — the loser is translated to a resume by the application service.
create unique index uq_tele_active_walkin
    on tele_session (beneficiary_id)
    where type = 'WALK_IN' and state in ('EM_FILA', 'EM_ATENDIMENTO');

-- One live SCHEDULED session per appointment, so re-joining the room resumes rather than duplicates.
create unique index uq_tele_active_scheduled
    on tele_session (appointment_id)
    where type = 'SCHEDULED' and state in ('EM_FILA', 'EM_ATENDIMENTO');


-- The symptoms selected at triage (BR2), a value collection of the session (codes from the symptom
-- registry). No behaviour of its own; recorded for the clinical summary.
create table tele_session_symptom (
    session_id uuid not null references tele_session (id),
    symptom_code varchar(40) not null references symptom (code),
    primary key (session_id, symptom_code)
);


-- Scheduled teleconsultation reuses domain.appointment (DL-0018): an ADDITIVE modality flag on the
-- appointment (not a new type — booking/cancel/reschedule/protocol all inherited) and an ADDITIVE
-- virtual flag on the care unit so a booking against the tele unit is recorded as TELEMEDICINA.
alter table appointment
    add column modality varchar(20) not null default 'PRESENCIAL'
        check (modality in ('PRESENCIAL', 'TELEMEDICINA'));

alter table care_unit
    add column virtual boolean not null default false;


-- Seeded virtual Telemedicina care unit (DL-0018): backs the tele agenda; not a physical address.
insert into care_unit
    (id, name, cep, street, address_number, complement, neighborhood, city, uf, phone, virtual)
values
    (
        'dd000000-0000-4000-8000-000000000001',
        'FKMed Telemedicina',
        null,
        null,
        null,
        null,
        'Atendimento remoto',
        'Rio de Janeiro',
        'RJ',
        '(21) 3333-0000',
        true
    );

-- The specialties offered by telemedicine (BR14). A subset of the domain.network registry suited to
-- remote care; each becomes a CONSULTATION agenda on the virtual unit.
insert into unit_agenda (id, unit_id, scope_type, scope_code)
select gen_random_uuid(), 'dd000000-0000-4000-8000-000000000001', 'CONSULTATION', sp.code
from specialty sp
where sp.code in (
    'CLINICA_MEDICA', 'PEDIATRIA', 'DERMATOLOGIA', 'PSIQUIATRIA', 'ENDOCRINOLOGIA'
);

-- Seed the tele agenda for the next 30 days, Mon-Sat, 08:00-16:30 in 30-minute slots (last start
-- 16:30, same business-hours window as the V16 in-person agenda). Dates are relative to current_date
-- so a fresh Testcontainers database always seeds "today -> +30 days" (SPEC-0010 BR14, mirroring the
-- V16 model). Capacity 3 per slot.
insert into schedule_slot (id, agenda_id, slot_date, slot_time, capacity, occupied, version)
select gen_random_uuid(), a.id, d::date, t::time, 3, 0, 0
from unit_agenda a
join care_unit u on u.id = a.unit_id and u.virtual = true
cross join generate_series(
    current_date::timestamp, (current_date + 29)::timestamp, interval '1 day'
) as d
cross join generate_series(
    timestamp '2000-01-01 08:00', timestamp '2000-01-01 16:30', interval '30 minutes'
) as t
where extract(isodow from d) <= 6;
