-- SPEC-0010 × SPEC-0011 (Phase 4 Wave 2): BE↔FE contract reconciliations aligning the backend to
-- the merged frontend's real consumption (minimize FE churn — the Phase-3 lesson).

-- 1) SPEC-0010 BR3 emergency-signal alert: the tele triage FE reads symptoms[].emergency to raise
-- the 24h-ER alert. An additive boolean on the symptom registry (baseline §0019 reference data),
-- defaulting false; the medically-obvious emergency signals are flagged true.
alter table symptom
    add column emergency boolean not null default false;

update symptom set emergency = true where code in ('DOR_TORACICA', 'FALTA_AR');

-- 2) SPEC-0011 BR6 referral detail: the FE renders the specialty NAME (specialtyName) and hands the
-- CODE (specialtyCode) to the SPEC-0009 wizard for pre-selection. A clinical document is an
-- immutable snapshot (BR8), so the specialty name is denormalized at issue time (resolved from the
-- domain.network registry by the issuer) rather than joined live — additive, nullable for the pre-
-- existing rows that only stored the code.
alter table clinical_document
    add column target_specialty_name varchar(120);

-- Backfill the seeded referrals (V18) whose name column did not yet exist at insert time, so the
-- referral detail renders the specialty name (BR6) instead of an empty field. Names mirror the
-- domain.network specialty registry (V15) for the seeded codes.
update clinical_document set target_specialty_name = 'Cardiologia'
    where id = 'dd000000-0000-4000-8000-000000000004';
update clinical_document set target_specialty_name = 'Dermatologia'
    where id = 'dd000000-0000-4000-8000-000000000008';
