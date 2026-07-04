-- SPEC-0002 (Identity and Access), slice 1.2: password recovery (BR10). One-time password-reset
-- link mirroring email_verification_token (SPEC-0002 §Persistence Changes): only the SHA-256 hash
-- is stored; the raw token travels in the reset e-mail. Single-use (used_at) and short-lived
-- (expires_at, 30 min). Lockout (failed_attempts, locked_until) reuses the columns already present
-- on user_account since V3 — no schema change is needed for BR8.

create table password_reset_token (
    id uuid primary key,
    account_id uuid not null references user_account (id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_password_reset_token_account on password_reset_token (account_id);
