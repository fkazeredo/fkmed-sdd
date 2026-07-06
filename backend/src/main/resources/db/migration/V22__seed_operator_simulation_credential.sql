-- SPEC-0018 (Operator Simulation API), Phase 4 tele slice (ADR-0017, DL-0021): a dev-seeded
-- INTERNAL OPERATOR credential the operator-simulation API (/api/sim/**) authenticates (BR2). It is
-- recognized as an OPERATOR_SIM by the config allowlist app.sim.operator-emails (dev/e2e only), never
-- by a beneficiary account. Backed here by a standalone disposable identity so the E2E/demo can log
-- in over HTTP (the guarded trigger ADR-0017 requires) — same disposable-identity pattern as the
-- seguranca-e2e seed (V7). Dev-only credential: operador-sim@fkmed.local / password `operador12345`
-- (enumerated in SECURITY.md, allowlisted in .gitleaks.toml). ProdReadinessValidator refuses to boot
-- the prod profile while this credential (or the app.sim.enabled flag) is present.
create extension if not exists pgcrypto;

insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active)
values (
    'c0000000-0000-4000-8000-000000000001',
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5a',
    'OPERADOR SIMULACAO',
    '52999999999',
    '700000000000099',
    '009000009',
    date '1980-01-01',
    'TITULAR',
    null,
    true
);

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at)
values (
    'c0000000-0000-4000-8000-000000000002',
    'c0000000-0000-4000-8000-000000000001',
    'operador-sim@fkmed.local',
    '{bcrypt}' || crypt('operador12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);
