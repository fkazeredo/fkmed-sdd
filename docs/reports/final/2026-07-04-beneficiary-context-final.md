# Relatório de conclusão — Slice 1.3 · Contexto do beneficiário, autorização & endurecimento de identidade

- **Fase/slice:** Fase 1 · Slice 1.3
- **Specs:** SPEC-0003 (BR1–BR5, BR8) + SPEC-0002 (débito A, BR8 sob concorrência)
- **Branch:** `feature/beneficiary-context` → PR para `develop`
- **Versão:** 0.4.0 (reconcilia também a 1.2 → 0.3.0, mergeada sem bump)
- **Data:** 2026-07-04

## 1. Critérios de aceite — evidência e porquê

| AC | Evidência (teste/comando/saída) | Por quê passou |
|---|---|---|
| **AC-1** (0003 AC1 · BR1/BR5) — MARIA vê self+PEDRO; PEDRO só self | `ContextApiIT.accessibleBeneficiaries_asMaria_returnsHerselfThenPedro` (200, `length=2`, [MARIA/TITULAR, PEDRO/DEPENDENT]) e `..._asPedro_returnsOnlyHimself` (`length=1`). FE: `beneficiary-selector.spec`, `i18n-completeness.spec` exercitando ambos os papéis. `./mvnw verify` verde. | `BeneficiaryAccess.accessibleEntities` = `[caller]` + dependentes **apenas se o caller é TITULAR**; MARIA (titular) → 2, PEDRO (dependente) → 1. Ordenação titular-primeiro reproduz `myPlanFor`. |
| **AC-2** (0003 AC2 · BR2/BR3) — PEDRO→MARIA por id → 404 sem vazar | `ContextApiIT.beneficiary_asPedro_ofMaria_returns404WithoutRevealingExistence` (404, `code=context.beneficiary-not-accessible`, msg neutra "Beneficiário não encontrado.") e `..._ofUnknownId_asMaria_returns404` (mesmo code). E2E `primeiro-acesso.spec`: token do PEDRO → `GET /api/context/beneficiaries/{MARIA}` → 404. | `summaryFor` filtra o alvo contra o conjunto acessível; miss → `BeneficiaryNotAccessibleException` (404). Alvo **fora de escopo** e **inexistente** retornam corpo idêntico → existência não revelada. |
| **AC-3** (0003 BR5) — trocar o beneficiário ativo atualiza o contexto | `beneficiary-context.service.spec` (setActive persiste em `sessionStorage` e restaura; default = TITULAR), `beneficiary-selector.spec` (onChange → `setActive`). | Contexto por signals; o seletor no header chama `setActive`; o signal `active` dirige o header. Servidor revalida cada request (BR3) — cliente é conveniência. |
| **AC-4** (0003 BR8) — DTOs sem CPF/CNS; ids em URL são UUID | Records `AccessibleBeneficiary`/`BeneficiarySummary` sem `cpf`/`cns`; `docs/api/openapi.json` regenerado mostra os schemas; rota `/{beneficiaryId}` é UUID. | DTOs propositalmente mínimos; a rota nunca carrega dado pessoal. Snapshot como contrato. |
| **AC-5** (débito A) — N falhas concorrentes ainda travam; conflito residual → 409, nunca crua | `ConcurrentFailedLoginIT` (8 threads, latch ready/fire): `failed_attempts=5`, `locked_until` setado, `identity.account-locked` auditado **1×**, **zero** exceções cruas. `HttpErrorMapping` mapeia `ConcurrentAccountUpdateException`→409; i18n `auth.concurrent-update`. PIT 87%/96%. | `@Version` (V6) + transação por-tentativa com retry (≤10) re-lê a versão fresca e reaplica o incremento; sem lost update. Vermelho sem `@Version` (documentado no IT). |
| **AC-6** (débito B) — E2E de segurança em conta descartável; MARIA intacta; sem ordem de arquivo; prod recusa | E2E 7/7 verde: `seguranca-conta.spec` usa `seguranca-e2e@fkmed.local`; `meu-plano.spec` loga a **MARIA com a senha original** (prova que não foi mutada). `ProdReadinessValidatorIT` recusa a conta descartável em prod. V7 semeia família própria. | O E2E de segurança mira a conta descartável; MARIA nunca é referenciada; a passagem do `meu-plano` prova a senha original intacta. O hack de nome-de-arquivo foi removido. |

Todos os 6 ACs **verdes com evidência**.

## 2. Retrospectiva do fluxo

**Timeline (handoffs):**
1. Arquiteto (opus/xhigh): plano da fase aprovado pelo owner; higiene (worktree obsoleta + ~19 branches mortas); slice aberta; contrato congelado.
2. Spawn paralelo: **dev-backend** (opus) em `--be` + **dev-frontend** (sonnet) em `--fe`, escopos disjuntos.
3. **dev-frontend → verde** (`172e5eb`): serviço de contexto + seletor + i18n + reescrita do E2E para conta descartável + negação de escopo. Gates FE verdes.
4. **Incidente de orquestração (dev-backend):** o agente gravou seus arquivos no **worktree principal** (em `develop`), não no seu worktree — caminho absoluto montado da raiz canônica do projeto, não do worktree. **Pego pelo arquiteto** numa checagem de `git status` (contagem de arquivos crescendo no main), **antes de qualquer commit ruim**. Agente parado; trabalho (2 débitos concluídos) **resgatado** para `--be` (`d2156c0`).
5. Arquiteto **finalizou o backend de contexto inline** (sem re-spawn frio) — `BeneficiaryAccess`, `ContextController`, exceção, DTOs, `ContextApiIT`, DL-0004/0005, mapeamento 404, i18n, snapshot (`fdc6ee6`). `./mvnw verify` verde.
6. Integração `--be` + `--fe` na slice (merges `--no-ff`, sem conflito); gates verdes (backend verify, FE lint/test/build, **E2E 7/7**, **PIT 87%/96%**).
7. Fresh-eyes (checklist da casa) → 1 achado **FE-1** (mensagem i18n contradizia BR2, dormante) → corrigido inline (`c22d7ec`).

**Reworks:** 1 (FE-1, menor, i18n neutra — BR2). Sem 2º REPROVADO.

**Gargalos:** (a) o incidente de worktree custou ~20 min + o resgate/finalização inline; causa-raiz = ferramentas de arquivo por caminho absoluto não ancoradas ao worktree do agente. (b) **Dívida de bookkeeping herdada da 1.2**: versão/CHANGELOG/MANUAL/ROADMAP-STATUS não haviam sido atualizados na 1.2 (mergeada sem `/dod` completo) — reconciliados aqui (0.3.0 + 0.4.0).

**Lições aprendidas:**
- **Salvaguarda de worktree** virou regra principal do workflow (PR #11): dev agents fazem *pin* do próprio worktree root (`git rev-parse --show-toplevel`, asserção `.claude/worktrees/agent-*`, só caminhos ancorados) e o arquiteto faz **landing check** ~1–2 min após o spawn. Também salvo na memória do arquiteto.
- **`/dod` precisa fechar seu bookkeeping** (versão + changelog + manual + status + report), senão a próxima slice herda a dívida.

## 3. Conteúdo padrão

- **Arquivos:** BE novos — `domain.plan/{BeneficiaryAccess, AccessibleBeneficiary, BeneficiarySummary, BeneficiaryNotAccessibleException}`, `application/api/ContextController`, `integration/{ContextApiIT, ConcurrentFailedLoginIT}`; BE modificados — `UserAccount` (`@Version`), `IdentityService` (retry), `ConcurrentAccountUpdateException`, `HttpErrorMapping`, `ProdReadinessValidator`, `messages.properties`, `SECURITY.md`, `.gitleaks.toml`; FE — `core/context/*`, `core/layout/beneficiary-selector*`, `shell*`, `i18n/translations.ts` (+completeness), `e2e/{seguranca-conta, primeiro-acesso}.spec.ts`; docs — `openapi.json`, `DL-0004/0005`, `CHANGELOG`, `MANUAL`, `ROADMAP-STATUS`, pom/OpenApiConfig (0.4.0).
- **Comportamento:** seletor de beneficiário ativo com escopo familiar server-side; endpoint de cartão por-beneficiário escopo-checado; conta concorrência-segura; E2E de segurança desacoplado da MARIA.
- **Specs/ADRs:** DL-0004 (posicionamento contexto em `domain.plan`, sem novo módulo), DL-0005 (tradução de conflito otimista). ADR-0001 (mapa de módulos) inalterado. BR9 (protocolo) adiado à Fase 3 (decisão do owner).
- **Migrations:** V6 (`version` em `user_account`), V7 (conta descartável dev-only).
- **Contratos:** `GET /api/context/accessible-beneficiaries`, `GET /api/context/beneficiaries/{id}` — snapshot regenerado.
- **Comandos:** `./mvnw verify` (verde, 102 cls), `./mvnw -Pmutation ... mutationCoverage` (87%/96%), `npm run lint && npm test && npm run build` (81), `npm run e2e` (7/7).
- **Riscos/pendências:** (1) **gate do owner**: revisar/Approve SPEC-0005 antes da 1.4 (Home). (2) PR #11 (safeguard) aguarda merge do owner. (3) versões 0.3.0/0.4.0 sem tag (ação humana). (4) 1.1 no ROADMAP-STATUS ainda diz "PR pending" apesar de mergeada (PR #3) — inconsistência pré-existente, não reescrita nesta slice.
