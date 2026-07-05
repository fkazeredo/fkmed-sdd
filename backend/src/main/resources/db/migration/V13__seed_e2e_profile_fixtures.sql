-- SPEC-0006 (Profile & Account), Phase 2: dedicated DISPOSABLE identities for the profile E2E
-- (perfil.spec.ts) and one seeded unread notification for MARIA (notificacoes.spec.ts), so the E2E
-- suite runs green over the isolated stack without mutating MARIA's canonical account for the
-- profile flows. Mirrors the disposable-account pattern of V7 (seguranca-e2e): standalone TITULAR
-- beneficiaries (each its own single-member family, titular_id null) on the existing plan, plus
-- ACTIVE user_account rows. Names, CPF, CNS, card numbers and contact data are FICTITIOUS POC
-- reference mass. Dev-only credentials: perfil-e2e@fkmed.local / password `perfilE2e12345` and
-- termos-e2e@fkmed.local / password `termosE2e12345` (enumerated in SECURITY.md, allowlisted in
-- .gitleaks.toml). ProdReadinessValidator refuses to boot the prod profile while these dev
-- credentials are present (same guard as the MARIA and seguranca-e2e seeds). Passwords are hashed
-- in-SQL by pgcrypto at migration time and stored with the delegating-encoder {bcrypt} prefix.
create extension if not exists pgcrypto;

-- (1) perfil-e2e: an ACTIVE, e-mail-verified TITULAR whose contact surface is freely mutated by
-- perfil.spec.ts. It accepts BOTH current legal documents (TERMS_OF_USE 1.0 + PRIVACY_POLICY 1.0),
-- so login is NOT intercepted (SPEC-0006 BR8). Contact e-mail + mobile are the mandatory contact
-- fields (BR6); contact_email equals the login e-mail here (they are independent by design).
insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active,
     contact_email, mobile)
values (
    'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b',
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
    'PERFIL E2E TESTE',
    '11122233396',
    '700000000000011',
    '009000002',
    date '1990-02-02',
    'TITULAR',
    null,
    true,
    'perfil-e2e@fkmed.local',
    '(21) 99999-0002'
);

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)
values (
    'f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c',
    'e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b',
    'perfil-e2e@fkmed.local',
    '{bcrypt}' || crypt('perfilE2e12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);

insert into term_acceptance (id, account_id, document_type, version, accepted_at) values
    (
        'c5d6e7f8-a9b0-4c1d-8e2f-3a4b5c6d7e8f',
        'f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c',
        'TERMS_OF_USE',
        '1.0',
        now()
    ),
    (
        'd6e7f8a9-b0c1-4d2e-9f3a-4b5c6d7e8f90',
        'f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c',
        'PRIVACY_POLICY',
        '1.0',
        now()
    );

-- (2) termos-e2e: an ACTIVE, e-mail-verified TITULAR that accepted ONLY PRIVACY_POLICY 1.0 and
-- NOT TERMS_OF_USE 1.0. The current Terms version is 1.0 (V12); not having accepted it is enough to
-- intercept THIS account on login (SPEC-0006 BR8) — no second version is published, which would
-- otherwise intercept every user. Contact e-mail + mobile satisfy the mandatory-contact rule (BR6).
insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active,
     contact_email, mobile)
values (
    'a3b4c5d6-e7f8-4a9b-8c0d-1e2f3a4b5c6d',
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
    'TERMOS E2E TESTE',
    '44455566619',
    '700000000000012',
    '009000003',
    date '1990-03-03',
    'TITULAR',
    null,
    true,
    'termos-e2e@fkmed.local',
    '(21) 99999-0003'
);

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)
values (
    'b4c5d6e7-f8a9-4b0c-9d1e-2f3a4b5c6d7e',
    'a3b4c5d6-e7f8-4a9b-8c0d-1e2f3a4b5c6d',
    'termos-e2e@fkmed.local',
    '{bcrypt}' || crypt('termosE2e12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);

insert into term_acceptance (id, account_id, document_type, version, accepted_at) values
    (
        'e7f8a9b0-c1d2-4e3f-8a4b-5c6d7e8f9012',
        'b4c5d6e7-f8a9-4b0c-9d1e-2f3a4b5c6d7e',
        'PRIVACY_POLICY',
        '1.0',
        now()
    );

-- (3) One seeded UNREAD in-app notification for MARIA (V3 account) so notificacoes.spec.ts sees
-- >= 1 unread in the bell/center over the isolated stack. Uses the account.password-changed type
-- (V10 catalog); title/body are the canonical pt-BR strings the in-app listener would produce
-- (messages.properties notification.account.password-changed.*), carrying no sensitive data (BR4);
-- link is null; read_at NULL marks it unread. The notification ITs wipe `notification` in
-- @BeforeEach, so this seed never perturbs their absolute counts (docs/architecture/testing.md).
insert into notification (id, account_id, event_type_code, title, body, link, created_at, read_at)
values (
    'f8a9b0c1-d2e3-4f4a-9b5c-6d7e8f901234',
    'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70',
    'account.password-changed',
    'Senha alterada',
    'A senha da sua conta foi alterada. Se não foi você, entre em contato imediatamente com nossos '
        || 'canais de atendimento.',
    null,
    now(),
    null
);
