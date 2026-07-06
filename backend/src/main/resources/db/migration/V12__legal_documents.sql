-- SPEC-0006 (Profile and Account), Phase 2: versioned legal documents (Terms of Use / Privacy
-- Notice) and their re-acceptance flow (BR8). Extends the acceptance area SPEC-0002 started: the
-- existing `term_acceptance` table (V3) already stores one immutable row per accepted version, so
-- versioned re-acceptance reuses it — this migration adds only the document catalogue and the
-- acceptance uniqueness guard, plus the current-version seed.

-- The document catalogue: one row per published version; the current version per type is the
-- latest by `published_at`. `type` (TERMS|PRIVACY) is a small fixed product catalogue kept as a
-- validated String code (baseline §0019), not a Java enum — the API path uses the same codes; the
-- acceptance row maps them to the SPEC-0002 document_type codes
-- (TERMS_OF_USE|PRIVACY_POLICY) so first-access acceptances and portal re-acceptances share one
-- history. History is preserved: superseding a version inserts a new row, never updates one.
create table legal_document (
    id uuid primary key,
    type varchar(20) not null check (type in ('TERMS', 'PRIVACY')),
    version varchar(20) not null,
    published_at timestamptz not null,
    body text not null,
    constraint uq_legal_document_type_version unique (type, version)
);

-- Seed the current versions (1.0), matching the app.legal defaults recorded at first access so a
-- freshly-registered user is already up to date (no spurious interception). Bodies are POC
-- placeholder text; the operator replaces them by publishing a new version row.
insert into legal_document (id, type, version, published_at, body) values
    (
        '2c1d0e9f-8a7b-4c6d-9e5f-0a1b2c3d4e5f',
        'TERMS',
        '1.0',
        timestamptz '2024-01-01 00:00:00+00',
        'Termos de Uso do portal do beneficiário FKMed (versão 1.0). Ao utilizar o portal, você '
            || 'concorda com as condições de acesso, uso responsável dos serviços e tratamento de '
            || 'dados descritos neste documento.'
    ),
    (
        '3d2e1f0a-9b8c-4d7e-8f6a-1b2c3d4e5f60',
        'PRIVACY',
        '1.0',
        timestamptz '2024-01-01 00:00:00+00',
        'Comunicado de Privacidade FKMed (versão 1.0). Descreve como coletamos, usamos e '
            || 'protegemos seus dados pessoais em conformidade com a LGPD, incluindo finalidades, '
            || 'compartilhamento e seus direitos como titular.'
    );

-- Acceptance uniqueness: a given account accepts a given document version at most once (the flow
-- is idempotent). Also indexes the lookups by (account, type, version) used to compute
-- "acceptedByMe".
alter table term_acceptance
    add constraint uq_term_acceptance_account_document_version
        unique (account_id, document_type, version);

-- MARIA (the canonical seeded account, V3) has accepted the current versions, so she is not
-- intercepted on login in dev. PEDRO has no seeded account (disposable test fixture), so nothing
-- to seed for him.
insert into term_acceptance (id, account_id, document_type, version, accepted_at) values
    (
        '4e3f2a1b-0c9d-4e8f-9a7b-2c3d4e5f6071',
        'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70',
        'TERMS_OF_USE',
        '1.0',
        now()
    ),
    (
        '5f4a3b2c-1d0e-4f9a-8b6c-3d4e5f607182',
        'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70',
        'PRIVACY_POLICY',
        '1.0',
        now()
    );
