-- Bug fix found while implementing SPEC-0014 AC2 (Home banner navigation, previously always
-- disabled — V8's seed data was never exercised by a real navigation until this slice wired it
-- up). The "Valide seu boleto" banner's destination (V8__content_home.sql) pointed to
-- '/financeiro#validar-boleto', a path that never matched a real frontend route (the module
-- ended up as domain.finance / '/financas', and the validator is its own screen, not an anchor
-- within a hub). Corrected to the real, existing route so the now-navigable banner does not
-- 404 (CLAUDE.md invariant 8 — every bug found requires a regression test; see home.spec.ts's
-- updated "renders the active banners" assertion).

update banner
set internal_destination = '/financas/validar'
where id = '3b2c4d5e-6f7a-4b2c-9d3e-4f5a6b7c8d9e';
