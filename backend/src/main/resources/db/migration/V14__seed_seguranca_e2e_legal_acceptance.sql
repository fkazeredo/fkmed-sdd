-- SPEC-0006 Phase 2 fix (cross-slice): the disposable account-security E2E identity
-- `seguranca-e2e@fkmed.local` was seeded in V7 (slice 1.3) BEFORE the legal-document catalogue
-- (V12) existed, so it has NO `term_acceptance` rows. SPEC-0006's terms-interception guard (BR8)
-- blocks any account whose current mandatory legal versions are unaccepted, so
-- seguranca-conta.spec.ts logged in but never reached Home (the acceptance screen intercepted it).
-- V12 seeded MARIA's acceptance and V13 seeded perfil-e2e's, but neither covered this V7 account.
--
-- Seed its acceptance of the CURRENT versions (TERMS_OF_USE 1.0 + PRIVACY_POLICY 1.0), mirroring
-- MARIA's V12 rows exactly (same document_type codes and version), so login is no longer
-- intercepted. `account_id` is the user_account seeded in V7 (`b2c3d4e5-…`).
--
-- Account coverage after this migration: MARIA (V12) and perfil-e2e (V13) accept both documents;
-- seguranca-e2e now accepts both; termos-e2e (V13) DELIBERATELY accepts only PRIVACY_POLICY so the
-- terms-interception test still has an intercepted account; PEDRO has no user_account. No seeded
-- account is left unaccepted-and-intercepted by accident.
insert into term_acceptance (id, account_id, document_type, version, accepted_at) values
    (
        '6a5b4c3d-2e1f-4a0b-9c8d-7e6f5a4b3c2e',
        'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e',
        'TERMS_OF_USE',
        '1.0',
        now()
    ),
    (
        '7b6c5d4e-3f2a-4b1c-8d9e-0f1a2b3c4d5e',
        'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e',
        'PRIVACY_POLICY',
        '1.0',
        now()
    );
