# Relatório de conclusão — Fase 3 · "Encontrar atendimento"

- **Data:** 2026-07-05
- **Specs:** SPEC-0008 (Busca de Rede Credenciada), SPEC-0009 (Agendamentos)
- **Branch / PR:** `feature/phase-3` → PR para `develop` (pendente)
- **Versão:** v0.7.0 (MINOR — duas capacidades novas retrocompatíveis; tag pendente do owner, §0023)
- **Base:** construída sobre `feature/phase-2` (PR #15, ainda não mergeado no início da fase)

## Resumo executivo

A Fase 3 entrega a jornada **"encontrar atendimento"**: o beneficiário **encontra prestadores** da
rede credenciada (funil localidade→tipo→especialidade + busca por nome, sempre dentro da **cobertura
do plano** e só entre prestadores **ativos**) e **marca, cancela e remarca consultas e exames** nas
unidades próprias da operadora, contra **capacidade real de horários sob concorrência**. É a primeira
fatia com **corrida por recurso** (a última vaga) e a primeira a **produzir eventos de negócio** que
alimentam a central de notificações da Fase 2.

Dois módulos Modulith novos: `domain.network` (8º, read-only, ADR-0011) e `domain.appointment` (9º,
transacional, ADR-0012). Tudo integrado em `feature/phase-3`, com backend `./mvnw verify` verde
(**413 testes**, Checkstyle 0, coverage nos pisos, ArchUnit + Modulith + snapshots OpenAPI/`modules.puml`)
e frontend `lint`/`test`/`build` verdes (**349 testes**, 92% statements). Entregue com **paralelismo
real** (4 agentes dev em 2 ondas, BE×FE), com a governança **commitada antes de ramificar** — a lição
da Fase 2 aplicada, sem colisão de numeração DL/ADR.

## Critérios de aceite (evidência)

### SPEC-0008 — Busca de Rede Credenciada

- **BR2/BR3 (funil derivado de prestadores ativos):** `/api/network/{states,municipalities,neighborhoods}`
  derivam as opções da base ativa; município aceita filtro texto (case/acento-insensível). Coberto por
  `NetworkSearchIT` + testes de repositório (`findDistinctActive*`).
- **BR4 (cobertura do plano):** `NetworkSearch` exige cobertura em cada passo (`requireCoverage`) e
  filtra UFs por `coverage.allowsUf`; `coverageFor` faz **fail-closed** (`PlanCoverage.NONE` quando o
  cartão é desconhecido). Modelo `plan.coverage`/`plan.coverage_uf` (V15, DL-0014). Erro
  `network.outside-coverage` (422).
- **BR5 (tipo de serviço com/sem passo de especialidade):** `ServiceTypeOption.hasSpecialtyStep`;
  especialidade enviada fora do passo é **zerada no servidor** (`clearSpecialtyOutsideItsStep`).
- **BR6/BR8 (especialidades / busca por nome ≥3):** catálogo alfabético; nome com <3 chars →
  `network.query-too-short` (422).
- **BR7 (resultados):** filtros exatos, só ativos, com data de referência; ordenação por nome.
- **BR12/BR13 (detalhe / indisponível):** `/providers/{id}` retorna 410 `network.provider-unavailable`
  para id desconhecido **ou inativo** (não revela inativo). Selos parametrizáveis (DL-0012).
- **Geografia IBGE (decisão do owner, DL-0014):** registry `municipality` semeado com a lista oficial
  (27 UFs + ~5.570 municípios, código IBGE + nome + FK UF).

### SPEC-0009 — Agendamentos

- **BR1 (escopo familiar):** `book` valida via `BeneficiaryAccess.requireAccessible`; `cancel`/`reschedule`/
  `list` via `accessibleFor` — fora de escopo → `AppointmentNotFoundException` (nunca revela existência).
- **BR5 + antecedência 2 h + horizonte 30 dias (DL-0013):** `BookingHorizon.isBookable` respeita
  hoje..+30 e ≥2 h; `availability` retorna vagas cheias com `remaining==0` (cliente renderiza indisponível).
- **BR6 (capacidade sob concorrência — o ponto crítico):** `ScheduleSlot.occupy()` + `@Version`;
  `occupySeat` traduz `OptimisticLockingFailureException` → `SlotUnavailableException` (409
  `appointment.slot-taken`), **fail-fast, sem retry**. Provado por `AppointmentConcurrencyIT` (slot
  capacidade-1, `CyclicBarrier(2)` + pool de 2 threads reais → exatamente **1** vence, o outro recebe o
  409; confere no banco `occupied==1` e 1 agendamento persistido).
- **BR7 (protocolo):** gerador compartilhado em `domain.plan` (DL-0016), sequência atômica, formato
  `AG-AAAAMMDD-####`.
- **BR4 (pedido médico no exame):** obrigatório; validado por **magic-bytes + assinatura `%PDF`** (DL-0015);
  ausente → `attachment-required` (422), inválido → `attachment-invalid` (422).
- **BR8 (conflito de horário):** mesmo beneficiário no mesmo instante (status ativo) → `time-conflict` (409).
- **BR9/BR10 (cancelar/remarcar):** cancelar libera a vaga e mantém no histórico; após o início →
  `cancel-too-late` (409). Remarcar ocupa a nova vaga (fail-fast), libera a antiga e **mantém o protocolo**.
- **BR11/BR12 (máquina de estados):** enum `AGENDADO→REAGENDADO|CANCELADO|REALIZADO` (invariante 7,
  keep-criterion no Javadoc); **REALIZADO derivado na leitura** (`effectiveStatus`), sem job.
- **BR13 (Meus Agendamentos):** `{upcoming,history}` divididos e ordenados no servidor, filtro por
  beneficiário acessível.

### Cross-spec (SPEC-0009 × SPEC-0004)

- `AppointmentConfirmed/Cancelled/Rescheduled` (AFTER_COMMIT) → `AppointmentNotificationListener`
  (`domain.notification`, `@TransactionalEventListener` + `REQUIRES_NEW`) vira notificação in-app +
  e-mail; corpo **sem PII** (protocolo, tipo, código, data, link `/agendamentos`). V17 semeia
  `appointment.cancelled`/`.rescheduled`. IT dedicada `AppointmentNotificationIT` (evento → in-app + e-mail).

## Governança

- **ADR-0011** (`domain.network`, mapa → 8) e **ADR-0012** (`domain.appointment`, mapa → 9, cobrindo
  concorrência de slot, gerador de protocolo e o compartilhamento da registry de especialidades).
- **DL-0012** (selos parametrizáveis), **DL-0013** (antecedência 2 h — decidido pelo owner),
  **DL-0014** (geografia IBGE + modelo de cobertura — decidido pelo owner), **DL-0015** (duplicar
  magic-byte + `%PDF` no `appointment`), **DL-0016** (gerador de protocolo em `domain.plan`).
- Specs 0008/0009 aprovadas e atualizadas como artefatos vivos (§Persistence da 0008 reescrita para a
  geografia IBGE; OQ1 da 0009 resolvida). `ModularityTest` +2 módulos; `modules.puml` regenerado com a
  aresta `notification → appointment`.

## Qualidade

- Backend: **413 testes**, 0 falhas; JaCoCo nos pisos (223 classes); ArchUnit + Spring Modulith verify;
  snapshot OpenAPI regenerado (drift gate verde) para 0.7.0.
- Frontend: **349 testes**, lint limpo, build ok; cobertura 92,49% statements / 83,88% branches.
- IT de concorrência real (não só unit) para o risco crítico — barra da Fase 2 mantida.

## Retrospectiva do workflow

**Linha do tempo (handoffs):** governança commitada **antes** de ramificar → Onda 1 (rede BE×FE, do
tip) → integração + reconciliação de contrato (arrays nus, `hasSpecialtyStep`, `locality` string única)
→ Onda 2 (agendamentos BE×FE, do tip da Onda 1 já com a registry de especialidades) → integração +
reconciliação (`medicalOrder`, `{upcoming,history}`, endpoint `/exams`) → cross-spec (listener +
V17 + IT) → gates finais → release lockstep → close-out.

**Reworks / reconciliações de contrato (do arquiteto, na integração):**
- Rede: FE assumia envelopes `{items:[...]}`; BE devolve **arrays nus** → FE re-sincronizado.
- Agendamento: FE usava `file`/lista plana; BE espera `medicalOrder`/`{upcoming,history}` e faltava
  `/exams` → ambos os devs reconciliados (SendMessage), padrão da Fase 2.

**Incidentes / gargalos:**
- Preocupação passageira com worktrees `worktree-agent-<id>` auto-criados em `main` — confirmado
  **não-problema** (o pin de caminho absoluto força o worktree correto); parei de narrar e segui.
- Nenhuma colisão de numeração DL/ADR (governança-antes-de-ramificar preveniu o problema da Fase 2).

**Lições aprendidas:**
- Commitar governança **antes** de ramificar sub-branches elimina a colisão de numeração.
- Congelar contratos como arrays/shape explícitos na work order reduz reconciliação — ainda houve
  divergência; vale anexar exemplos JSON literais na próxima fase.
- Ramificar a Onda 2 do tip integrado da Onda 1 entregou a registry de especialidades pronta, sem
  conflito cross-branch.

## Riscos e pendências

- **Observações fresh-eyes (menores, não bloqueiam):** (1) `AppointmentOutsideHorizonException`
  reusada para código de especialidade/exame inválido e agenda inexistente — edge-case-only (funil/
  catálogo garantem código válido); (2) `NetworkSearch.detail` não é escopado por cobertura — aceitável
  (diretório de prestador não é PII por-beneficiário; BR13 satisfeito).
- **E2E/PIT/CodeQL/gitleaks** rodam autoritativos no CI no push; monitoro até verde e faço triagem de
  qualquer vermelho (padrão da Fase 2).
- Merge do PR e tag v0.7.0 são ações **humanas** do owner (§0023). Recomendo mergear antes o PR #15
  (Fase 2) para o PR da Fase 3 ficar limpo contra `develop`.

## Desfecho do CI (pós-abertura do PR)

_A preencher após o push e a corrida do CI (checks verdes, contagens finais de E2E/PIT)._
