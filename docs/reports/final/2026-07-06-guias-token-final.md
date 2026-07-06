# Relatório de conclusão — Fatia 5.1 "Guias e Token"

**Fase 5 · Fatia 1/3** · Specs: SPEC-0012 (Guides and Tokens), SPEC-0018 (Operator Simulation —
ações de guia). Branch `feature/guias-token` → PR para `develop`. Versão **v0.9.0**. Migração
**V23**. Módulo novo **`domain.guides`** (12º), ADR-0018.

## 1. Critérios de aceite (evidência + porquê)

| AC | Evidência (teste/comando/saída) | Por que passou |
|---|---|---|
| **AC-1** (BR2) MARIA vê 3 guias com badges distintos | `GuideApiIT#list_asMaria_returnsThreeGuidesWithDistinctStatuses…` (IT Testcontainers) + `guias-hub.spec.ts` (badges) | O seed V23 cria 3 guias para MARIA (EM_ANALISE, AUTORIZADA, NEGADA); a lista retorna array puro ordenado por data desc e a UI renderiza uma classe de badge por status. |
| **AC-2** (BR3) PEDRO (sem guias) vê estado vazio | E2E `guias.spec.ts` "AC2: a dependent with no guides…" (verde) + `guias-hub.spec.ts` (empty state) | Troca de beneficiário para PEDRO recarrega a lista (BR1); array vazio ⇒ `guias-vazio` com ícone de chave + "Atualizar informações", nunca tela em branco. |
| **AC-3** (BR5) detalhe autorizado mostra senha/validade; negado mostra motivo | `GuideApiIT` (detalhe autorizada/negada) + `guide-detail.spec.ts` + E2E AC7 (mostra `AUT-777001` + validade) | O detalhe expõe `authPassword`/`authValidUntil` só quando AUTORIZADA/PARCIALMENTE e `denialReason` quando NEGADA; a UI renderiza a caixa de autorização e a de negativa condicionalmente. |
| **AC-4** (BR9/BR11) token 6 dígitos + contagem 10:00; copiar = código exato | `TokenApiIT` (geração) + `token-time.spec.ts` (mm:ss) + E2E AC4/AC5 (`code` casa `^\d{6}$`, clipboard == código) | `POST /api/tokens` devolve `{code(6),expiresAt(+10min)}`; a UI roda a contagem client-side por `expiresAt`; copiar usa o clipboard e confirma. |
| **AC-5** (BR9) regenerar invalida o anterior | `TokenApiIT#generate_again_invalidatesThePreviousToken_ac5` (IT) + E2E AC4/AC5 (`code2 != code1`) | A geração seta `invalidated_at` do token ativo (via `saveAndFlush`, ver bug abaixo) e o índice único parcial `WHERE invalidated_at IS NULL` garante um só válido por beneficiário. |
| **AC-6** (BR10) expirado → "Token expirado", código antigo nunca válido | `TokenApiIT` (`/current` 404 após expirar) + `token-time.spec.ts` (`isTokenExpired`) | `GET /api/tokens/current` responde 404 `token.none-active` quando não há token não-invalidado e não-expirado; a UI mostra o estado `token-expirado` e oferece renovar. |
| **AC-7** (BR4/BR8) operador autoriza via sim → notifica + lista reflete após Atualizar | `OperatorSimGuidesIT` (autorizar → notificação criada + detalhe com senha, indistinguível) + E2E AC7 (sim autoriza → Atualizar → AUTORIZADA → senha) | O `/api/sim/guides/{id}/authorize` dirige a máquina de estados e publica `GuideStatusChanged`; `GuideStatusChangedListener` cria a notificação (número + status, sem dado clínico); a lista relê o novo status. |

Todos os 7 ACs com evidência verde — DoD fecha.

## 2. Retrospectiva do fluxo

**Timeline (branch sempre visível):**
1. **Gate de spec** — SPEC-0012 e SPEC-0018 (sem Open Questions) → Approved; branch `feature/guias-token` a partir de `develop`.
2. **Contrato congelado** (arquiteto) + sub-branches `--be`/`--fe`.
3. **BE (`dev-backend`, sonnet, `--be`) ∥ FE (`dev-frontend`, sonnet, `--fe`)** em paralelo (worktrees isolados). O BE achou e corrigiu um **bug real** (ordem de flush do Hibernate violava o índice único parcial na regeneração do token → `saveAndFlush`), com regressão em 2 camadas (unit + IT). Ambos entregaram verdes (BE 658, FE 484).
4. **Integração** (merge `--no-ff` de `--be` depois `--fe`) — a reconciliação de contrato pegou **3 divergências FE↔BE** (envelope da lista, enum de período, `beneficiaryId` do detalhe). **Rework 1** ao mesmo Dev Frontend (contexto preservado) → verde.
5. **E2E** — autorei `guias.spec.ts` (AC7 sim-driven, AC4/AC5 token, AC2 vazio); a suíte completa (29) ficou verde sobre seed fresco após corrigir um **bug de isolamento no meu próprio teste** de token (assumia estado inicial sem token no stack compartilhado).
6. **Review fresh-eyes** (agente read-only) — **1 Blocker + 2 Important + 3 Minor**. Corrigidos inline com regressão: (1) colisão de nome de schema OpenAPI `Item` (guia sobrescrevia o `ClinicalDocumentListResponse.Item` do SPEC-0011) → rename para `GuideItemRequest`; (2) `GuideService` retornava a entidade `Guide` à camada de delivery → passa a devolver `GuideTransitionResult`; (6) `@Positive` na quantidade do item. Backend re-verificado verde + E2E de guias re-rodado verde sobre o artefato final.
7. **Fechamento** — ADR-0018, release v0.9.0 (lockstep + changelog + manual cap.14), este relatório, PR.

**Reworks e razões:** (a) 1 rework de reconciliação de contrato ao FE — **causa raiz: falha de orquestração minha** (o arquivo do contrato congelado vive em `docs/reports/plans/`, que é gitignored, então não chegou ao worktree isolado do FE; ele caiu na spec e marcou 3 palpites). (b) Correções do review (blocker + 2 achados) — feitas inline pelo arquiteto.

**Gargalos:** o ciclo de rebuild+regeneração do snapshot para o blocker de colisão de schema; a reconciliação FE↔BE (evitável se o contrato tivesse chegado ao worktree).

**Lições aprendidas (aplicar já na 5.2/5.3):**
- **Inlinar o contrato congelado COMPLETO em toda ordem de trabalho** — nunca depender do arquivo de plano gitignored; worktrees isolados não o recebem. (Na 5.1 o BE recebeu o contrato inline e ficou firme; o FE recebeu só a referência ao arquivo e teve rework.)
- **Vigiar colisões de nome de schema OpenAPI** ao criar DTOs com nomes simples comuns (`Item`, `Result`…): o springdoc chaveia por simple name e sobrescreve silenciosamente. Guardado agora por `OpenApiSnapshotIT#clinicalDocumentListItemSchema_isNotOverwrittenByASchemaNameCollision`.
- **Testes E2E no stack compartilhado devem ser agnósticos ao estado** para recursos mutáveis (token): clicar o botão de gerar presente, não assumir estado inicial.

## 3. Conteúdo padrão

- **Arquivos** — BE: `domain/guides/*` (novo módulo: `Guide`/`GuideItem`/`AttendanceToken`/`GuideService`/`TokenService`/`GuideStatusChanged`/`GuideTransitionResult`/enums/exceptions), `application/api/{GuideController,TokenController}`, `application/sim/*Guide*` + `SimService` (extensão), `domain/notification/GuideStatusChangedListener`, `infra/web/HttpErrorMapping` (+2), `V23__guides.sql`. FE: `features/guias/*` (hub lista+token, detalhe, api, format/time), `app.routes.ts`, `core/layout/shell` (nav-guias), `core/i18n/translations.ts`. E2E: `e2e/guias.spec.ts`.
- **Comportamento** — transparência de guias (lista/filtros/detalhe/notificação) + token antifraude (gerar/copiar/expirar/renovar); operator-sim dirige as guias.
- **Specs/ADRs** — SPEC-0012/0018 → Approved; ADR-0018 (mapa de módulos `domain.guides`).
- **Testes** — BE unit+IT (derivação de status, token single-valid/expiry, `OperatorSimGuidesIT`) + 2 regressões novas do review (`OpenApiSnapshotIT` colisão, `SimCreateGuideRequestTest` quantidade) + regressão do bug de flush (2 camadas). FE 484 (Vitest). E2E 29 (fresh seed) incl. 3 de guias.
- **Migração** — V23 (`guide`, `guide_item`, `attendance_token` + índice parcial único + seed MARIA 3 / PEDRO 0 + tipo de notificação).
- **Contrato** — `/api/guides`, `/api/guides/{id}`, `/api/tokens`, `/api/tokens/current`, `/api/sim/guides/*`; snapshot `docs/api/openapi.json` regenerado em 0.9.0.
- **Comandos** — `./mvnw -Dopenapi.snapshot.write=true verify` (EXIT 0); `npm run lint && npm test && npm run build` (484 verde); `npm run e2e:up && npx playwright test` (29/29 + guias 3/3 pós-fix).
- **Verificação** — todos os gates verdes na fatia integrada + após as correções do review.

## 4. Riscos e pendências

- **[Owner] Finding 3 do review — BR5 "(ou item negado)":** hoje o `denialReason` só aparece quando o status geral é NEGADA; uma guia **PARCIALMENTE_AUTORIZADA com item negado** não exibe motivo (o `partially-authorize` do sim nem aceita `reason`). É nuance de interpretação de negócio (Rule #1) — **trago a você**: aceite consciente como fora-de-escopo do POC, ou follow-up (adiciona `reason` opcional ao `partially-authorize` + expõe `denialReason` no parcial + UI). Não bloqueia a jornada principal.
- **Finding 4 (Minor, aceito):** keep-criterion de `GuideType` é um recorte de escopo do produto (3 dos tipos TISS), não "fixo por lei" estrito — o ADR-0018 registra a expansão como gatilho de revisão (vira registry code se crescer).
- **Finding 5 (Minor, aceito):** `GuideApiIT` assere contagem absoluta sobre o seed no Postgres compartilhado; a limpeza existe via convenção do `OperatorSimGuidesIT` — fragilidade a endurecer numa fatia futura (não é defeito hoje).
- **Débito herdado:** consolidação de `target_specialty_name` na V18 (Fase 4) segue pendente para uma fase futura.
