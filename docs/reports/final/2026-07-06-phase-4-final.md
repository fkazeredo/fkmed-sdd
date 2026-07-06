# Relatório de conclusão — Fase 4 · "Cuidado digital"

- **Data:** 2026-07-06
- **Specs:** SPEC-0010 (Telemedicina), SPEC-0011 (Documentos Clínicos / Minha Saúde), SPEC-0018
  (Operator Simulation — **apenas o slice de telemedicina+documentos**)
- **Branch / PR:** `feature/phase-4` → PR para `develop`
- **Versão:** v0.8.0 (MINOR — capacidades novas; tag pendente do owner, §0023)

## Resumo executivo

A Fase 4 entrega a jornada **"cuidado digital"**: o beneficiário faz **Pronto Atendimento** por
telemedicina (triagem → termo → **fila ao vivo (SSE)** → sala → encerramento) e **teleconsulta
agendada**, e os documentos emitidos aparecem em **Minha Saúde** (receitas, atestados, encaminhamentos,
solicitações de exame) com filtros, detalhe, **PDF** e "agendar consulta" a partir de um encaminhamento.
O lado do profissional é dirigido por um **slice mínimo, flag-gated, da SPEC-0018** (o operator-sim),
o que torna a jornada completa demonstrável de ponta a ponta.

Dois módulos Modulith novos — `domain.clinicaldocs` (10º) e `domain.telemedicine` (11º) — mais a 1ª
superfície **SSE (push)** do codebase. Tudo integrado em `feature/phase-4`, com backend `./mvnw verify`
verde (**601 testes**, Checkstyle 0, coverage nos pisos, PIT ≥ 60, ArchUnit + Modulith + snapshots) e
frontend `lint`/`test`/`build` verdes (~**451 testes**). Entregue com **paralelismo real** (4 agentes na
Onda 1 + 1 na Onda 2) sobre a governança commitada **antes de ramificar** (lição das Fases 2–3).

## Decisões do owner (regra #1 — AskUserQuestion)

- **Sala state-driven, sem mídia** (OQ1/0010, ADR-0015) — WebRTC é pós-POC.
- **CID exibido nos atestados** (OQ1/0011, DL-0020) — *override* do default de privacidade da spec.
- **Fila em push via SSE** (BR6, ADR-0016/DL-0022) — 1ª infra de streaming.
- **Driver tele mínimo agora** (DL-0021/ADR-0017) — só o slice tele+docs da SPEC-0018; o resto na Fase 5.

## Critérios de aceite (evidência)

- **AC1/AC6 (fila SSE / join)** — posição/ETA por `SseEmitter` (re-emissão periódica); join tele habilita
  10 min antes. *E2E + ITs (`TeleApiIT`, `TeleSchedulingIT`, `TeleSessionStreamIT`).*
- **AC2 (sair)** — `fila-sair` → `ABANDONADA`, hub reabre. *E2E `tele.spec` + IT.*
- **AC3 (no-show 5 min)** — sweep + regra de domínio contra o `Clock`. *IT `TeleSessionLifecycleIT`.*
- **AC4 (encerramento → receita em Minha Saúde)** — o sim `close` emite os documentos **atômico** (uma
  transação, BR10) e dispara `TeleSessionClosed` + `ClinicalDocumentIssued`. *IT `OperatorSimTeleIT`
  (close com 2 docs + specialtyName do registry + auditoria + notificações in-app/e-mail) + E2E dirigido
  pelo sim.*
- **AC5 (sessão única)** — 2º Pronto Atendimento retoma a existente (optimistic lock). *IT + unit.*
- **AC7 (banner instabilidade)** — banner no hub/fila (SPEC-0005). *unit FE.*
- **AC8 (minor por titular)** — sessão do dependente, autor auditado. *IT.*
- **AC9 (Minha Saúde)** — filtros beneficiário+período, selo Expirado (ainda baixa PDF), detalhe por tipo
  (**+CID**), referral→wizard com especialidade pré-selecionada, auditoria de dependente. *E2E
  `minha-saude.spec` + ITs `ClinicalDocumentApiIT`/`ClinicalDocumentIssuanceIT`.*
- **AC10 (sim guard rails)** — flag-off → 404, beneficiário → 403, transição inválida → 409, prod
  fail-fast. *ITs `OperatorSimTeleIT`/`OperatorSimDisabledIT` + `ProdReadinessValidatorTest`.*

**Nota E2E:** os specs Playwright (`tele.spec` jornada dois-atores dirigida pelo sim; `minha-saude.spec`)
foram **autorados** e são validados/monitorados no CI; a jornada de backend já está provada pelas ITs
acima (`OperatorSimTeleIT` cobre encerramento→documentos→notificações de ponta a ponta).

## Governança

- **ADR-0013** (clinicaldocs), **ADR-0014** (telemedicine), **ADR-0015** (sala state-driven), **ADR-0016**
  (SSE), **ADR-0017** (operator-sim seam). **DL-0017..0022**. Specs 0010/0011 aprovadas com as decisões do
  owner; SPEC-0018 anotada (slice tele nasce na Fase 4). `ModularityTest` +2 módulos; `modules.puml`
  regenerado (arestas `notification→telemedicine/clinicaldocs`, `telemedicine→clinicaldocs`).

## Qualidade

- Backend **601 testes**, PIT ≥ 60 (unit tests de domínio dos serviços — lição da Fase 3). Frontend ~**451**.
- IT de encerramento→documentos **atômica** + IT de concorrência (sessão única) — barra das Fases 2–3.

## Retrospectiva do workflow

**Linha do tempo:** governança-antes-de-ramificar → Onda 1 (4 agentes: clinicaldocs BE×FE + telemedicine
BE×FE, escopos disjuntos) → integração (conflitos em HttpErrorMapping/ModularityTest + routes/shell/
translations, resolvidos mantendo os dois módulos) → Onda 2 (1 dev: sim + cross-spec + reconciliações
BE→FE) → deltas de FE (badge por `modality`, parse UTC do ticker) → release lockstep → E2E autorado.

**Reworks / achados:**
- 2 handbacks de BE voltaram **incompletos** (o agente parou esperando o `verify` em background): o
  `docs-be` teve 1 violação de Checkstyle (switch sem `default`) que corrigi inline; o `wave2-be` só
  precisou eu finalizar o verify + commit/push. Trabalho no disco, nada perdido — verificado antes de
  confiar em "done" (regra do architect.md).
- Reconciliações de contrato FE↔BE (padrão da Fase 3): o dev da Onda 2 **alinhou o BE ao FE** em quase
  tudo (emergency, room, escopo teleconsulta, detail), deixando só 1 delta obrigatório de FE (badge por
  `modality`) — aplicado.

**Lições aplicadas da Fase 3:** governança antes de ramificar; unit tests de domínio para o PIT; alinhar
o BE ao FE na reconciliação (minimiza churn de FE); landing check dos worktrees (main ficou limpo).

## Riscos e pendências

- **SSE** é infra nova — a jornada dois-atores (fila avança quando o sim atende) é o ponto mais novo;
  coberta por ITs + o E2E monitorado no CI.
- **E2E Playwright** validado/triado no CI (não pré-validei localmente dado o tamanho da sessão; a jornada
  backend está IT-provada). Trio qualquer vermelho como nas Fases 2–3.
- **Emergency symptoms** (`DOR_TORACICA`/`FALTA_AR`) são default de produto do dev — confirmar com o owner
  se quer outros.
- Merge do PR e tag v0.8.0 são ações **humanas** do owner (§0023).

## Desfecho do CI (pós-abertura do PR)

O PR #17 exigiu **3 rodadas de triagem** até o verde:

- **Rodada 1** (3 vermelhos): isolamento do `OperatorSimTeleIT` (apagava o seed V18 em bloco →
  quebrava o `ClinicalDocumentSeedIT` por ordem de teste); build FE (`TS2353` no campo
  `AppointmentView.telemedicine`); E2E (selector do card `<a>` + **SSE bufferizado pelo nginx** →
  `X-Accel-Buffering: no`).
- **Rodada 2** (BE verify + PIT + E2E): BE verify e PIT eram a **mesma cascata de Spotless** (reflow
  de comentário no `OperatorSimTeleIT`, commitado sem `spotless:apply`); o E2E revelou 3 defeitos
  reais só reproduzíveis num stack fresco do worktree (ver Débito técnico).
- **Rodada 3** (fixes validados localmente): backfill do `target_specialty_name` no V21; token do
  operador-sim lido do `sessionStorage` (não Home); fluxo de saída da fila do AC2; card com validade
  no minha-saúde. **tele 2/2 e minha-saúde 3/3 verdes localmente** antes do push.

## Débito técnico (registro por ordem do owner)

> Ordem do owner (2026-07-06): *"esses problemas de CI viraram débito técnico — anote"*. Registrado
> aqui de forma durável.

**Débito de processo (a causa-raiz comum).** A suíte **E2E do Playwright foi entregue/integrada sem
nunca ter rodado verde num stack real** — os "gates verdes" dos devs cobriram unit/build, não uma
corrida E2E completa. Consequência: **6 defeitos** só apareceram no CI (e vários só na reprodução
local com stack do worktree), custando 3 rodadas de triagem. O risco estava **explicitamente
sinalizado** neste relatório (Riscos: *"não pré-validei o E2E localmente"*) e se materializou.
_Mitigação para as próximas fases:_ **rodar a suíte E2E localmente até o verde antes de devolver a
fatia** (incluir no gate de dev/QA), e **`spotless:apply` obrigatório no pré-commit** do worktree.

**Defeitos encontrados (todos corrigidos nesta fase) e o débito residual:**

| # | Defeito | Camada | Correção | Débito residual |
|---|---|---|---|---|
| 1 | SSE bufferizado pelo nginx (sala presa em "Conectando…") | infra/BE | `X-Accel-Buffering: no` na resposta SSE | — (fix production-correct) |
| 2 | `OperatorSimTeleIT` apagava o seed V18 em bloco | teste BE | delete escopado às linhas do IT | — |
| 3 | Spotless commitado sem aplicar → cascata BE verify + PIT | processo | `spotless:apply` | pré-commit não barrou no worktree |
| 4 | `target_specialty_name` criado no V21 (reconciliação) e **seed V18 deixado nulo** → detalhe do encaminhamento vazio | schema/seed | backfill no V21 | **a coluna pertence logicamente ao CREATE TABLE do V18**; consolidar (migrations ainda não liberadas) ficou adiado p/ não re-editar o V18 |
| 5 | `operatorToken` esperava Home, mas operador-sim cai no interceptor `/aceite-legal` (não é beneficiário onboarded) | teste E2E | esperar o token no `sessionStorage` | credencial de sim não tem caminho de token dedicado (aceitável — dev-only) |
| 6 | Teste do AC2 esperava tela ABANDONADA; o componente navega direto ao hub (a tela serve o AC3 no-show) | teste E2E | alinhar o teste ao comportamento (Rule Zero) | — |

**Itens de acompanhamento (abrir como issues na próxima fase):**
- Consolidar `target_specialty_name` no V18 (mover a coluna do V21 para o CREATE TABLE + semear o
  nome direto), enquanto as migrations da Fase 4 ainda não foram liberadas em ambiente algum.
- Gate de dev/QA: E2E completo local verde + `spotless:apply` no pré-commit, para não repetir o
  padrão de defeitos-que-só-aparecem-no-CI.
