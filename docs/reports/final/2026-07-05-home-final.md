# Relatório de conclusão — Slice 1.4 · Home (SPEC-0005) — fecha a Fase 1

- **Fase/slice:** Fase 1 · Slice 1.4 (última da fase)
- **Spec:** SPEC-0005 (aprovada nesta slice, com nota de entrega faseada)
- **Branch:** `feature/home` → PR para `develop`
- **Versão:** 0.5.0
- **Data:** 2026-07-05

## 1. Critérios de aceite — evidência e porquê

| AC | Evidência | Por quê passou |
|---|---|---|
| **AC-1** (0005 AC1 · BR1/BR2) — MARIA → Home "Olá, MARIA", plano, cartão `001234567` | E2E `home.spec.ts` (`card-greeting`/`card-plan-name`/`card-number`) + unit `home.spec.ts`. | O cartão lê `context.active()` e busca o resumo via `/api/context/beneficiaries/{id}`; greeting em maiúsculo do primeiro nome. |
| **AC-2** (0005 AC5 · BR1) — trocar p/ PEDRO → cartão do PEDRO (**jornada-título**) | E2E: troca no seletor → `card-greeting` "Olá, PEDRO", cartão `001234575`; unit exercita a reatividade com `BeneficiaryContextService` real + `HttpTestingController`. | Um `effect()` refaz o fetch a cada mudança do beneficiário ativo. |
| **AC-3** (0005 AC3 · BR6) — banner fora de validade ausente | `BannerTest` (antes/no início/dentro/no fim/depois/limites nulos/inativo) + `ContentApiIT` (expirado/futuro excluídos, válidos na ordem). | Filtro server-side via `Clock` no facade `HomeContent`. |
| **AC-4** (0005 AC4 · BR7) — accordion single-open + severidade | E2E (`notice-2` ativo / `notice-1` inativo ao abrir) + unit. | PrimeNG Accordion `multiple=false`; ALERT com tag visual distinta. |
| **AC-5** (0005 BR3/BR4) — 9 atalhos na ordem, "em breve", Reconhecimento Facial dialog | unit `home.spec.ts` (ordem exata; só Reconhecimento Facial habilitado) + E2E (`shortcut-carteirinha` disabled + "Em breve"). | Lista fixa; não-entregues `disabled` com tag; Reconhecimento Facial abre diálogo. |
| **AC-6** (0005 BR8) — falha de conteúdo → seções ocultas, cartão+atalhos seguem | unit `home.spec.ts` (500 e arrays vazios). | Erro/vazio oculta só a seção afetada. |
| **AC-7** (deferral, decisão do owner) — 0005 AC2/AC6 diferidos p/ Fases 2/5 | Nota de entrega faseada na SPEC-0005; E2E asseta "Em breve". | Destinos não entregues nesta fase. |

Gates: backend verify **234 testes** · **PIT 83% mortas / 96% força** · frontend **101 testes** · **E2E 8/8**.

## 2. Retrospectiva do fluxo

- **Paralelo BE×FE** (sonnet/sonnet), contrato `/api/content/home` congelado. Ambos com **pin de worktree obrigatório**; **landing check** ~1 min após spawn **confirmou os dois nos próprios worktrees e o main limpo** — o incidente da 1.3 **não se repetiu** (safeguard validado no campo).
- **dev-backend** (`ec3586b`): `domain.content` + V8 + `/api/content/home` + ADR-0006 (pegou que ADR-0005 já existia). **dev-frontend** (`a9d3674`): Home + redirect pós-login + ajuste dos 3 E2E existentes.
- **Reworks/achados:** (a) 1 falha de E2E na integração — asserção do banner casava 2 nós (clone do carousel `[circular]`) → corrigida com `.first()`, consistente com as asserções vizinhas; (b) 1 flake de timeout no teste de completude i18n **causado por mim** ao rodar backend verify + frontend em paralelo — passou sozinho; lição: **não paralelizar gates pesados**.
- **Gargalo:** o ciclo extra de E2E pela asserção do banner. **Lição:** rodar gates sequencialmente.

## 3. Conteúdo padrão

- **Backend:** `domain.content` (`Banner`, `Notice`, `NoticeSeverity`, repos, `HomeContent`, `HomeContentResponse`), `ContentController`, `V8__content_home.sql` (+seed BR9), `BannerTest`, `ContentApiIT`, `ModularityTest` (5 módulos), `modules.puml`, `openapi.json`.
- **Frontend:** `features/home/*`, `core/context/beneficiary-summary.api.ts`, `app.routes.ts` (redirect → Home + "Início"), `shell`, i18n (+completeness), `e2e/home.spec.ts` + 3 specs ajustados.
- **ADR:** ADR-0006 (módulo `domain.content`, revisa ADR-0001). **Migrations:** V8. **Contrato:** `GET /api/content/home` (snapshot regenerado). **Versão:** 0.5.0.
- **Riscos/pendências:** versões 0.3.0–0.5.0 sem tag (ação humana do owner). Fase 2 (0004/0006/0007) ainda `Draft` — abre após aprovação/Open Questions. Detalhe menor: `[circular]` no carousel gera clone aria-hidden (por isso o `.first()` no E2E) — funcional; simplificável removendo `[circular]` se/quando incomodar.

**Fase 1 fechada** (jornada completa: criar conta → login → Home com cartão → trocar beneficiário ativo).
