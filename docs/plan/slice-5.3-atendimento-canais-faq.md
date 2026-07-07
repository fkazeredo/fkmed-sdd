# Plano — Slice 5.3 (Canais + FAQ) + endurecimento do workflow de subagentes

## Context

A Fase 5 ("Plano e finanças", `ROADMAP.md`) cobre as specs **0012, 0018, 0013, 0014**. Já
entregues: **5.1 guias-token** (0012/0018) e **5.2 finanças** (0013/0018). O que resta para
**fechar a Fase 5** é a spec **0014 — Service Channels and FAQ**, ou seja a **slice 5.3**:
uma página de Atendimento com canais, seção antifraude (destino do banner de golpe da Home),
Central de Libras e um FAQ pesquisável.

Em paralelo, este ciclo de trabalho expôs falhas reais no uso de subagentes de background
(um Explore agent morreu silenciosamente quando o turno foi interrompido para trocar de
modelo; o executor afirmou "ainda rodando" sem verificar; ficamos esperando um agente já
morto). O owner pediu para **gravar guardrails nos `.md`** de forma explícita. Este plano
entrega as duas coisas em **duas PRs independentes**.

Decisão do owner nesta sessão:
- **OQ1 (0014)**: usar **placeholders fictícios** para números de canal e horário da Libras,
  documentados e trocáveis por migration futura. Não bloqueia.
- **Workflow-docs**: **4 regras literais separadas**, em **CLAUDE.md + workflow.md**; a
  regra 1 fica como **preferência** (não override duro).

## Escopo

### Workstream 1 — Guardrails de subagente (docs, PR própria)
Branch `docs/background-agent-guardrails` → PR para `develop`. Docs-only, sem código de
produto. **4 regras literais** (texto abaixo), em **CLAUDE.md** (sempre carregado, com dente)
+ racional em **`docs/architecture/workflow.md`**.

### Workstream 2 — Slice 5.3 (produto, PR própria)
Branch `feature/atendimento-canais-faq` → PR para `develop`. Novo módulo `domain.support`
(14º), migration **V25**, **ADR-0021**, 4 endpoints REST, página `/atendimento` no frontend,
bump **v0.11.0**. Fecha a Fase 5.

## Fora de escopo
- Chat ao vivo com atendentes; abertura de protocolo de ouvidoria pelo portal; condução da
  videochamada de Libras (equipe da operadora) — conforme "Out of Scope" da 0014.
- Débitos técnicos (owner confirmou que não há para tratar agora).
- Dados reais de contato/horário (placeholders fictícios por decisão do owner — OQ1).

---

## Workstream 1 — texto das 4 regras (versão final)

### Em CLAUDE.md — nova subseção `### Background subagents (visibility guardrails)` logo após "Lean SDD operating mode"

1. **Prefer direct reads for narrow-scope context.** When you can already name the files or
   docs a slice needs, reading them yourself is usually faster and cheaper than spawning an
   Explore/search subagent. Reach for a context subagent when scope is genuinely uncertain or
   spread across the repo, or for independent parallel work.
2. **An interrupted turn's background subagents are dead.** Interrupting a turn — including a
   model switch or a re-sent message — cancels the background agents that turn spawned, with
   no notification to the main conversation. After any interrupt, treat those agents as gone:
   re-verify liveness or re-spawn deliberately before relying on them.
3. **Assert liveness only after checking it — never infer it from silence.** The absence of a
   completion notification is not evidence that an agent is running. Confirm before telling
   the owner an agent is "still working"; if you cannot confirm, say "não consigo confirmar"
   instead of guessing.
4. **Never leave the owner waiting on an opaque agent.** Do not block on a background agent
   for context you could gather inline. When you do spawn one, announce it, keep working in
   the main thread meanwhile, and report unknown or stale status the moment you notice it —
   silence is not a status update.

### Em docs/architecture/workflow.md — nova seção `## Background subagents and context gathering` (racional), referenciada na Routing Map

> The main executor usually gathers context fastest by reading directly. An Explore/search
> subagent tends to be worth its cold-start cost when scope is genuinely uncertain or spread
> repo-wide, or when the work is independent enough to run in parallel — less so for a slice
> whose files you can already name (rule 1). Background subagents are fragile across turn
> boundaries: interrupting the current turn (stopping it, switching model, re-sending the
> prompt) cancels the subagents it spawned, and the main conversation gets no kill
> notification (rule 2). So never infer liveness from a missing completion notification —
> that silence is indistinguishable from a dead agent; verify before reporting an agent as
> running, and if you cannot, say so (rule 3). Owner visibility comes first: announce async
> spawns, keep working inline instead of blocking, and surface unknown or stale status
> immediately (rule 4).
>
> **Origin:** written after a slice where an Explore agent — spawned to read a single-file
> spec — was silently killed by a model-switch interrupt while the executor kept reporting it
> as "still running", costing the owner a blind wait on a dead process.

Gate: nenhum (docs). Validação: revisão visual + `git diff`. Sem bump (docs-only, regra
`/release`).

---

## Workstream 2 — Slice 5.3

### Critérios de aceite (spec 0014)
- **AC1** (BR5) — buscar "reembolso" no FAQ mostra só perguntas relacionadas; limpar
  restaura a lista completa.
- **AC2** (BR3) — clicar "Saiba mais" no banner de golpe da Home abre esta página
  posicionada na seção antifraude (`/atendimento#antifraude`).
- **AC3** (BR5) — abrir uma pergunta com outra aberta fecha a anterior (accordion single-open).
- **AC4** (BR4) — "Solicitar atendimento em Libras" dentro do horário registra e mostra o
  próximo passo; fora do horário mostra o horário e oferece registro para o próximo período.
- **AC5** (BR1) — card do WhatsApp abre o chat com o número oficial em nova aba.

### Decisões autônomas de design (registrar via `/dl` antes de codificar)
- **DL — codes, não enums de negócio** (§0019): `ChannelTypeCodes`
  (CENTRAL/WHATSAPP/OUVIDORIA/ANS) e `FaqCategoryCodes` (6 categorias) como *constants
  holders* de `String` validada (precedente `SymptomDurationCodes`), fixos por estrutura de
  produto; **não** tabela registry (evita overengineering, Rule Zero) e **não** enum. A
  situação do Libras (`REGISTERED|ATTENDED`) é ciclo de vida → **enum**
  `LibrasRequestSituation` (só `REGISTERED` é setado no POC).
- **DL — armazenamento do conteúdo antifraude**: a seção "Persistence Changes" da 0014 não
  lista tabela para o conteúdo antifraude, embora o Business Context o marque como conteúdo
  semeado. Semear cabeçalho/mensagem operáveis em `support_antifraud` (linha única); as 3
  boas-práticas + link do validador são copy/estrutura no frontend (BR2 proíbe *contato*
  hardcoded, não guidance). **Atualizar a seção Persistence da spec** no mesmo PR (artefato
  vivo).
- **DL — janela de horário da Libras (OQ1)**: placeholder seg–sex 08:00–18:00
  America/Sao_Paulo via property `fkmed.support.libras.*` (trocável sem migration), lógica
  dentro/fora com `Clock` injetado (precedente `HomeContent`).
- **FAQ real-time**: frontend chama `GET /api/support/faq?q=&category=` com debounce; o
  servidor filtra (normalização acento/caixa via `java.text.Normalizer`) e **conta buscas
  com zero resultado** (contador de observabilidade). Satisfaz AC1 + teste de contrato +
  teste domain + observability num só mecanismo.

### Ordem de implementação (vertical, BE → FE)
1. **Migration V25** `V25__support_channels_and_faq.sql`: tabelas `support_channel`
   (type/label/value/hours/display_order), `faq_entry` (category-code/question≤200/answer/
   display_order/active), `libras_request` (beneficiary_id/requested_at/situation), e
   `support_antifraud` (title/message). Seeds: canais BR1 (placeholders fictícios), **≥12
   FAQ nas 6 categorias incl. ≥3 Reembolso** alinhadas às regras de reembolso (prazo 12
   meses, documentação por tipo, preview não-vinculante — BR6), conteúdo antifraude BR3.
2. **Módulo `com.fkmed.domain.support`**: `package-info.java` (`@ApplicationModule "support"`,
   ADR-0021); entidades + repositórios; `ChannelTypeCodes`/`FaqCategoryCodes`/
   `LibrasRequestSituation`; `SupportService` (busca FAQ normalizada; janela Libras com
   `Clock`; auditoria via `AuditRecorder`; contadores via `MeterRegistry`); `SupportController`
   com os 4 endpoints; DTOs **prefixados** (`SupportChannelResponse`, `AntifraudResponse`,
   `FaqEntryResponse`, `LibrasRequestResponse`, `LibrasRequestCommand`) para evitar colisão
   de schema OpenAPI (lição da 5.1).
   - Endpoints: `GET /api/support/channels`, `GET /api/support/antifraud`,
     `GET /api/support/faq` (q+category), `POST /api/support/libras-requests` (beneficiaryId
     scope-checked, 201 com nextStep dentro/fora do horário, auditado).
3. **Frontend `features/atendimento/`** (espelha `features/financas/`): `atendimento-hub`
   (cards de canal BR1 + seção `id="antifraude"` BR3, destino do banner), `faq` (busca
   debounced + filtros de categoria + accordion single-open), `libras` (explicação +
   "Solicitar atendimento em Libras" + confirmação); `support.api.ts`; rotas em
   `app.routes.ts` (`atendimento`, `atendimento/faq`, `atendimento/libras`). Garantir
   `withInMemoryScrolling({anchorScrolling:'enabled'})` no router para o fragmento
   `#antifraude` (AC2). i18n em `core/i18n/translations.ts`.
4. **Teste-âncora** (cedo): `SupportApiIT` (Testcontainers) provando `GET /faq?q=` com
   normalização acento/caixa **e** `POST /libras-requests` dentro/fora do horário (Clock
   fixo) persistido **e auditado**, com scope-check.
5. **Demais testes**: `SupportServiceTest` (matching acento/caixa; janela de horário);
   contrato/snapshot dos 4 endpoints; frontend unit (posicionamento da âncora; accordion
   single-open; combo busca+categoria; links tap-to-call/WhatsApp); **E2E**
   `e2e/tests/atendimento.spec.ts` (banner→âncora AC2; jornada de busca FAQ AC1; confirmação
   Libras AC4).
6. **Fechamento**: `/release` bump **v0.11.0** (pom.xml + versão hardcoded no `OpenApiConfig`
   + regen snapshot OpenAPI + CHANGELOG); ADR-0021; DLs; spec 0014 Draft→Approved + ajuste
   Persistence; `/manual` (docs/MANUAL.md — página Atendimento); linha em `ROADMAP-STATUS.md`.

### Arquivos-chave
- `backend/src/main/resources/db/migration/V25__support_channels_and_faq.sql`
- `backend/src/main/java/com/fkmed/domain/support/**` (novo módulo)
- `backend/src/test/java/.../support/SupportApiIT.java`, `SupportServiceTest.java`
- `frontend/src/app/features/atendimento/**` (novo), `frontend/src/app/app.routes.ts`,
  `frontend/src/app/core/i18n/translations.ts`
- `frontend/e2e/tests/atendimento.spec.ts`
- Docs: `docs/specs/0014-service-channels-and-faq.md`, `docs/adr/0021-*`, `docs/MANUAL.md`,
  `docs/ROADMAP-STATUS.md`, `docs/decision-log/*` + `INDEX.md`

### Gates proporcionais / comandos de validação
```bash
cd backend && ./mvnw spotless:apply && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down   # E2E: jornada nova
```
PIT/mutation **não aplicável** (módulo content-serving, sem lógica de dinheiro/cálculo
crítico) — conforme gates enxutos do workflow.

### Reviewer / QA
- **Reviewer**: sim ao final (fresh-eyes) — módulo novo, novo contrato, migration+seed,
  superfície de front nova. Baixo risco mas vale olhar de fora (colisão de schema, boundary).
- **QA**: **não** aciono. Sem dinheiro/LGPD sensível além do padrão; a única escrita
  (Libras request) é scope-checked + auditada e coberta por IT. Se o Reviewer levantar algo
  de escopo/segurança, reavalio.

### Riscos
- **AC2 anchor scrolling**: fragmento `#antifraude` exige `anchorScrolling` no router; se não
  estiver ligado, ativar (risco baixo, verificável cedo).
- **Normalização de acento no BE**: garantir consistência (`Normalizer.Form.NFD` + strip
  diacríticos + lower) — coberto por teste domain.
- **Boundary/Modulith/ArchUnit**: módulo novo precisa passar no verify sem vazamento
  cross-module; DTOs prefixados contra colisão de schema OpenAPI.
- **Banner já semeado (V8)** aponta para `/atendimento#antifraude`: a rota **precisa** existir
  com esse path e âncora, senão o link da Home fica quebrado (regressão de Fase 1).

## Fluxo de Git (convenção do projeto)
- Dois branches de `develop`; commits locais convencionais; **push só quando verde**; abrir
  **duas PRs para `develop`**. O agente **não** faz merge, tag nem force-push (DECISIONS
  BASELINE §0023). Owner mergeia.

## Verificação end-to-end
1. `./mvnw verify` verde (incl. `SupportApiIT` + `SupportServiceTest` + contrato).
2. `npm run lint && npm test && npm run build` verde.
3. Stack isolada + seed fresco: `npm run e2e` — jornada Atendimento verde **local** (banner
   da Home → âncora antifraude; busca FAQ "reembolso" filtra e limpa restaura; accordion
   single-open; solicitar Libras dentro/fora do horário; WhatsApp abre em nova aba).
4. `/dev-env` opcional para checagem manual da página `/atendimento`.
5. `/dod`: confere ACs + evidência do teste-âncora, gates, DoD, docs vivos, abre PR.
