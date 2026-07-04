-- SPEC-0002 (Identity and Access), slice 1.1: user accounts, first-access verification,
-- e-mail verification tokens and term acceptances. The account lifecycle is a state machine
-- (EMAIL_NOT_VERIFIED -> ACTIVE). Lockout columns (failed_attempts, locked_until) exist now but
-- are owned by SLICE 1.2 (BR8). Cross-context link: beneficiary_id is a value with a DB-level FK
-- for integrity only — no JPA relationship crosses the module boundary (ADR-0001).

create table user_account (
    id uuid primary key,
    beneficiary_id uuid not null unique references beneficiary (id),
    email varchar(160) not null unique,
    password_hash varchar(200) not null,
    status varchar(20) not null check (status in ('EMAIL_NOT_VERIFIED', 'ACTIVE')),
    failed_attempts int not null default 0,
    locked_until timestamptz,
    created_at timestamptz not null default now(),
    constraint chk_user_account_email_format check (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

create index idx_user_account_email on user_account (email);

-- One-time e-mail verification link (SPEC-0002 BR5): only the SHA-256 hash is stored; the raw
-- token travels in the verification e-mail. A resend invalidates previous links (used_at set).
create table email_verification_token (
    id uuid primary key,
    account_id uuid not null references user_account (id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_email_verification_token_account on email_verification_token (account_id);

-- Acceptance of the current Terms of Use / Privacy Policy at registration (SPEC-0002 BR15).
-- document_type is a String code (LegalDocumentTypes), not an enum (baseline §0019); the legal
-- documents themselves (pages, re-acceptance on new versions) are owned by SPEC-0006 (DL-0001).
create table term_acceptance (
    id uuid primary key,
    account_id uuid not null references user_account (id),
    document_type varchar(40) not null,
    version varchar(20) not null,
    accepted_at timestamptz not null default now()
);

create index idx_term_acceptance_account on term_acceptance (account_id);

-- Seed MARIA's real ACTIVE account (replaces the retired in-memory dev-login seam of SPEC-0001
-- BR8). Dev-only credential: e-mail maria@fkmed.local / password `maria12345` (enumerated in
-- SECURITY.md, allowlisted in .gitleaks.toml). The hash is a fresh-salt BCrypt produced by
-- pgcrypto at migration time and stored with the delegating-encoder {bcrypt} prefix.
-- ProdReadinessValidator refuses to boot the prod profile while this dev credential is present.
create extension if not exists pgcrypto;

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)
values (
    'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70',
    '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c',
    'maria@fkmed.local',
    '{bcrypt}' || crypt('maria12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);
