# Relatório de conclusão — Fatia 5.2 "Plano › Finanças"

**Fase 5 · Fatia 2/3** · Spec: SPEC-0013 (Plan Finance, Approved; OQ1 resolvida) + SPEC-0018 (ações
de finança). Branch `feature/financas` → PR para `develop`. Versão **v0.10.0**. Migração **V24**.
Módulo novo **`domain.finance`** (13º), ADR-0019; ADR-0020 (operator-sim full API). **Primeira fatia
sob o workflow novo** (1 developer ponta a ponta + QA em dois estágios).

## 1. Critérios de aceite (evidência + porquê)

| AC | Evidência (teste/comando/saída) | Por que passou |
|---|---|---|
| **AC-1** (BR2) abas Em aberto (atual + vencido destacado) / Pagos (2) | `FinanceApiIT` (tabs/ordenação) + homologação QA ao vivo (browser+V24) | Status derivado (paid/overdue/open); vencido traz o **valor atualizado** (multa+juros); abertos/pagos só o original. |
| **AC-2** (BR3) copiar linha (47 díg) + PIX com confirmação | `boleto-detalhe.spec.ts` (cópia exata) + homologação QA | Detalhe expõe `digitableLine`(47) e `pixCode`; a UI copia exatamente e confirma. |
| **AC-3** (BR4) validador autêntico (com espaços) + não-reconhecido do-not-pay + 422 | `FinanceApiIT` (validador) + `InvoiceValidation`/`DigitableLine` unit + PIT 0 sobreviventes + homologação | Normaliza → exige 47 díg (senão 422 antes do lookup); compara com as emitidas; NOT_RECOGNIZED nunca sugere pagar. |
| **AC-4** (BR5) copay filtrado (período+PEDRO) → tabela+total recalculado | `FinanceApiIT` (copay filtros) + `financas` FE spec + homologação | Filtros recomputam linhas+total; cobre a família ou individual. |
| **AC-5** (BR6) IR PDF (12 meses + total) | `TaxStatementPdfRenderer` + `FinanceApiIT` (anos+PDF) + homologação | Anos com pagamento; PDF agrega 12 meses (zeros) + total anual. |
| **AC-6** (BR1) dependente ⇒ sem cartão + negação | `FinanceApiIT` (403 dependente) + `finance-denied` FE + E2E (troca p/ PEDRO) | Backend 403 `finance.titular-only` por caller; FE oculta/nega pelo **beneficiário ativo** (decisão do owner). |
| **AC-7** (BR3) 2ª via de paga com marca "PAGO" | `InvoicePdfRendererTest` (marca presente em pago / ausente em aberto) + homologação | O renderer OpenPDF estampa "PAGO" quando `paidAt` existe. |
| **AC-8** (BR7) ano quitado oferece declaração; ano com aberto não (409) | `FinanceApiIT` (anos+PDF+409) + homologação | Seed: 2025 totalmente pago ⇒ oferece; 2026 com aberta ⇒ 409 `finance.year-not-settled`. |

Todos os 8 ACs com evidência verde (homologação QA APROVADA + bateria VERDE). DoD fecha.

## 2. Retrospectiva do fluxo (primeira fatia no workflow novo)

**Timeline:**
1. **Gate de spec** (arquiteto) — SPEC-0013 → Approved aplicando a decisão do owner de OQ1 (exibir juros/multa: multa 2% + juros 1%/mês pro rata die); branch `feature/financas` de `develop@4dca1ac`.
2. **1 `developer` (opus) ponta a ponta** (`feature/financas`) — backend (`domain.finance` + validador + juros/multa + 3 PDFs + sim-finance + V24) → snapshot real → frontend (6 telas) → i18n; testou todas as camadas + E2E + gates **ao final**. Achou/decidiu 3 pontos técnicos (levados como perguntas): `varchar(47)`+check, negação por beneficiário ativo, barcode FEBRABAN. **Entregou verde** (710 BE / 507 FE / 30 E2E), push `0d93fd8`. ~83 min (a fatia mais pesada da fase, um só agente).
3. **Owner** confirmou a decisão de negócio (negação pelo beneficiário ativo); o arquiteto aprovou as 2 técnicas.
4. **QA (opus), dois estágios** — **Homologação APROVADA (0 itens)**: AC1–AC8 ao vivo + exploratório (validador, idempotência, fronteiras de juros, LGPD sem log de linha/PIX, passe adversarial nos testes). **Bateria VERDE**: `verify` 710/0, **PIT** (calculadoras money 0 sobreviventes), front 507, E2E 30.
5. **Review fresh-eyes** (agente read-only) — **"Aprovar com ressalvas"**: 1 Important (contadores de observabilidade da §Observability da SPEC-0013 não implementados — validador por resultado/sinal antifraude + downloads de PDF por tipo) + 2 Minor (citação de BR da idempotência: SPEC-0013→SPEC-0018 BR6; duplicação leve entre PDF renderers). O arquiteto **corrigiu inline** o Important (contadores via `MeterRegistry`, padrão do `TokenService`, + regressão `FinanceServiceTest#validate_countsUsesByResult`) e o Minor de citação; o Minor de duplicação foi **aceito** (o revisor confirmou que NÃO é violação de Rule Zero). Backend re-verificado verde.
6. **Fechamento** (arquiteto) — ADR-0019/0020, release v0.10.0, manual cap.15, este relatório, PR.

**Reworks:** nenhum de developer. A homologação passou de primeira (contraste com a 5.1, que teve 1 rework de contrato — porque lá havia split BE∥FE e o contrato congelado não chegou ao worktree; aqui, **1 developer ponta a ponta = sem contract-freeze, o snapshot real emergiu backend-first**, exatamente o ganho do fluxo novo).

**Gargalos / lições:**
- **Visibilidade**: o build de um agente só (fatia pesada) durou ~83 min e eu deixei o owner sem ping observável no meio — corrigido (a partir daqui, ping de estado observável, sem silêncio).
- O fluxo novo (1 developer end-to-end) **eliminou a reconciliação de contrato** que custou um rework na 5.1 — ganho real de performance/qualidade.
- QA de dois estágios funcionou: a homologação spec-first deu confiança antes de gastar a bateria pesada (PIT/E2E).

## 3. Conteúdo padrão

- **Arquivos** — BE: `domain/finance/*` (novo módulo: `Invoice`/`CopayEntry`/`FinanceService`/`Invoices`/`Copays`/`UpdatedAmount`/`DigitableLine`/`InvoiceValidation`/`Competencia`/3 PDF renderers/enums/exceptions), `application/api/FinanceController`, `application/sim/*` (extensão finança), `domain/notification/InvoiceIssuedListener`, `infra/web/HttpErrorMapping` (+4), `V24__finance.sql`. FE: `features/financas/*` (hub, boleto-detalhe, validar-boleto, coparticipacao, imposto-renda, quitacao, finance-denied, api), `app.routes.ts`, `shell` (nav-financas), `core/i18n/translations.ts`. E2E: `e2e/financas.spec.ts`.
- **Comportamento** — Finanças titular-only: boletos/PIX/2ª via, validador antifraude, coparticipação, IR, Lei 12.007; operator-sim gera/paga(idempotente)/copay.
- **Specs/ADRs** — SPEC-0013 Approved (OQ1 resolvida); ADR-0019 (`domain.finance`), ADR-0020 (operator-sim full API, estende ADR-0017).
- **Testes** — 710 BE (unit money-críticas + ITs + `OperatorSimFinanceIT` idempotência) + **PIT** (money calcs 0 sobreviventes). 507 FE. 30 E2E (fresh seed).
- **Migração** — V24 (`invoice` `digitable_line varchar(47)`+check único, `copay_entry`, tipo de notificação, seed MARIA 2025 pago / 2026 aberto+vencido + 8 copays).
- **Contrato** — `/api/finance/**` + `/api/sim/finance/*`; snapshot regenerado em **0.10.0**.
- **Comandos** — `./mvnw -Dopenapi.snapshot.write=true verify` (EXIT 0); PIT BUILD SUCCESS; `npm run lint/test/build`; `npm run e2e:up && e2e` (30/30). Todos verdes.
- **Verificação** — QA dois estágios (homologação APROVADA + bateria VERDE) + fresh-eyes.

## 4. Riscos e pendências

- **Fora de escopo (QA) — teste flaky na fatia Guias (5.1, já mergeada)**: `guias-hub.spec.ts:178` (countdown `09:59`≠`10:00` sob carga). Passa na reexecução; não é desta fatia. **Disposição do arquiteto**: endurecer com fake timer/clock injetável — NÃO misturado no PR de Finanças; agendado como débito (fix rápido separado ou na 5.3).
- **Fora de escopo (QA/dev) — warning de bundle** (901,79 kB vs teto de aviso 900 kB, +1,79 kB): cumulativo do projeto, build passa (só warning). Disposição: ajustar o teto de aviso numa manutenção; não é gate quebrado.
- **PIT**: sobreviventes concentrados nas fachadas de escrita (`Invoices`/`Copays`) e montagem de PDF — cobertos por ITs verdes (PIT não seleciona a camada de integração); nenhuma calculadora money com sobrevivente; nenhum limiar estourado.
- **Débito herdado**: `target_specialty_name` na V18 (Fase 4) segue pendente.
