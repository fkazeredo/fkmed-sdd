-- SPEC-0006 (Profile and Account), Phase 2: beneficiary-owned contact/address data (BR5/BR6), the
-- profile photo (BR2/BR3) and the seeded UF registry (baseline §0019 — never an enum). Contract
-- data (name, CPF, birth date, card, plan) stays where SPEC-0001 owns it and remains read-only
-- (BR4); only the beneficiary-editable contact surface is added here.

-- UF registry: the 27 Brazilian federative units as reference data (code + editable label), the
-- registry the contact UF is validated against (baseline §0019). Seeded by migration; the JSON
-- contract carries the stable 2-letter code.
create table uf_registry (
    code varchar(2) primary key,
    name varchar(40) not null
);

insert into uf_registry (code, name) values
    ('AC', 'Acre'),
    ('AL', 'Alagoas'),
    ('AP', 'Amapá'),
    ('AM', 'Amazonas'),
    ('BA', 'Bahia'),
    ('CE', 'Ceará'),
    ('DF', 'Distrito Federal'),
    ('ES', 'Espírito Santo'),
    ('GO', 'Goiás'),
    ('MA', 'Maranhão'),
    ('MT', 'Mato Grosso'),
    ('MS', 'Mato Grosso do Sul'),
    ('MG', 'Minas Gerais'),
    ('PA', 'Pará'),
    ('PB', 'Paraíba'),
    ('PR', 'Paraná'),
    ('PE', 'Pernambuco'),
    ('PI', 'Piauí'),
    ('RJ', 'Rio de Janeiro'),
    ('RN', 'Rio Grande do Norte'),
    ('RS', 'Rio Grande do Sul'),
    ('RO', 'Rondônia'),
    ('RR', 'Roraima'),
    ('SC', 'Santa Catarina'),
    ('SP', 'São Paulo'),
    ('SE', 'Sergipe'),
    ('TO', 'Tocantins');

-- Beneficiary-owned contact/address columns (BR5). All nullable so existing rows migrate cleanly;
-- the mandatory-field rule (contact e-mail + mobile, BR6) is a domain invariant enforced on every
-- write, not a NOT NULL that would break the migration on legacy rows without contacts yet. Column
-- `address_number` avoids the SQL word "number"; the JSON field stays `number`. Format checks
-- mirror the domain validators (SPEC-0006 §Validation Rules) as an integrity backstop.
alter table beneficiary
    add column contact_email varchar(160),
    add column mobile varchar(20),
    add column landline varchar(20),
    add column cep varchar(8),
    add column street varchar(120),
    add column address_number varchar(10),
    add column complement varchar(60),
    add column neighborhood varchar(80),
    add column city varchar(80),
    add column uf varchar(2) references uf_registry (code);

alter table beneficiary
    add constraint chk_beneficiary_contact_email
        check (contact_email is null or contact_email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    add constraint chk_beneficiary_mobile
        check (mobile is null or mobile ~ '^\(\d{2}\) \d{5}-\d{4}$'),
    add constraint chk_beneficiary_landline
        check (landline is null or landline ~ '^\(\d{2}\) \d{4}-\d{4}$'),
    add constraint chk_beneficiary_cep
        check (cep is null or cep ~ '^\d{8}$');

-- Profile photo, one per beneficiary (BR3). Stored as bytes + the sniffed content type
-- (magic-byte validated in the domain, never the extension — BR2); overwriting replaces the image.
create table beneficiary_photo (
    beneficiary_id uuid primary key references beneficiary (id),
    image bytea not null,
    content_type varchar(20) not null check (content_type in ('image/jpeg', 'image/png')),
    updated_at timestamptz not null default now()
);

-- Contact defaults for the canonical seed (SPEC-0001 MARIA/PEDRO), consistent with the RJ plan.
-- Contact e-mail is independent from the login e-mail (BR6); fictitious POC reference mass.
update beneficiary set
    contact_email = 'maria.contato@fkmed.local',
    mobile = '(21) 99876-5432',
    landline = '(21) 2222-1010',
    cep = '20040002',
    street = 'Avenida Rio Branco',
    address_number = '156',
    complement = 'Sala 801',
    neighborhood = 'Centro',
    city = 'Rio de Janeiro',
    uf = 'RJ'
where id = '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c';

update beneficiary set
    contact_email = 'pedro.contato@fkmed.local',
    mobile = '(21) 98765-4321',
    cep = '20040002',
    street = 'Avenida Rio Branco',
    address_number = '156',
    complement = 'Sala 801',
    neighborhood = 'Centro',
    city = 'Rio de Janeiro',
    uf = 'RJ'
where id = '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d';
