-- Débito técnico B (SPEC-0003 slice 1.3): a dedicated DISPOSABLE identity for the account-security
-- E2E (seguranca-conta.spec.ts) so it stops mutating/locking MARIA's canonical account.
-- A standalone TITULAR beneficiary (its own single-member family, titular_id null) on the existing
-- plan, plus an ACTIVE user_account. Names, CPF, CNS and card number are FICTITIOUS POC reference
-- mass. Dev-only credential: seguranca-e2e@fkmed.local / password `seguranca12345` (enumerated in
-- SECURITY.md, allowlisted in .gitleaks.toml). ProdReadinessValidator refuses to boot the prod
-- profile while this dev credential is present (same guard as the MARIA seed).
create extension if not exists pgcrypto;

insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active)
values (
    'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
    'SEGURANCA E2E TESTE',
    '11144477735',
    '700000000000010',
    '009000001',
    date '1990-01-01',
    'TITULAR',
    null,
    true
);

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)
values (
    'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e',
    'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d',
    'seguranca-e2e@fkmed.local',
    '{bcrypt}' || crypt('seguranca12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);
