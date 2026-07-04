-- SPEC-0003 (Beneficiary Context and Authorization) — audit foundation (slice 1.1 subset).
-- Append-only, immutable trail (BR6/BR7): no UPDATE or record-level DELETE path exists in the
-- application; the only sanctioned removal is the coarse 12-month retention sweep (BR10).
-- author_account_id / target_beneficiary_id are cross-context id VALUES (no FK): the trail
-- outlives the accounts/beneficiaries it references. details is masked JSON (BR8 — no CPF/CNS).

create table audit_event (
    id uuid primary key,
    occurred_at timestamptz not null,
    author_account_id uuid,
    target_beneficiary_id uuid,
    event_type varchar(60) not null,
    details jsonb not null default '{}'::jsonb,
    ip varchar(45),
    user_agent varchar(400)
);

-- Lookup by target beneficiary over time (BR6 audit queries) and the retention sweep (BR10).
create index idx_audit_event_target_time on audit_event (target_beneficiary_id, occurred_at);
create index idx_audit_event_occurred_at on audit_event (occurred_at);
