# Relatório de conclusão — Fase 2 · "Minha conta e identificação"

- **Data:** 2026-07-05
- **Specs:** SPEC-0004 (Notificações), SPEC-0006 (Perfil e Conta), SPEC-0007 (Carteirinha Digital)
- **Branch / PR:** `feature/phase-2` → PR para `develop` (pendente)
- **Versão:** v0.6.0 (MINOR — features novas; tag pendente do owner, §0023)
- **Extras entregues no mesmo PR (por ordem do owner):** correção do alerta CodeQL (cartão em
  texto claro no JWT) e mudança de `AuditRetentionJob` de `infra` para `application`.

## Resumo executivo

A Fase 2 entrega a jornada de **conta e identificação** do beneficiário: uma **central de
notificações** (sino + tela + preferências) que centraliza avisos in-app e e-mail; a área de
**Perfil** (foto por beneficiário, edição de contatos auditada, documentos legais versionados com
interceptação, sair); e a **carteirinha digital** (cartão + ficha + PDF + "Minhas Carteirinhas").
Ainda no mesmo PR, o número da carteirinha saiu do token JWT (fix de segurança CodeQL) e o job de
retenção foi realocado para a camada de entrega. Tudo integrado em `feature/phase-2`, com backend
`./mvnw verify` verde (**321 testes**, Checkstyle 0, coverage nos pisos, ArchUnit + Modulith +
snapshots) e frontend `lint`/`test`/`build` verdes (**204 testes**). Entregue com **paralelismo real**
(7 agentes dev em 2 ondas, BE×FE), recuperado de um limite transitório de sessão da API no meio da
Onda 2, sem perda de trabalho.

## Critérios de aceite (evidência)

### SPEC-0004 — Notificações

| AC | Como foi verificado | Evidência |
|---|---|---|
| AC1 (evento → e-mail + item não lido) | Integração Testcontainers | `NotificationContactChangedIT` (contact-changed → item in-app + e-mail antigo e novo) e `PasswordChangedNotificationListener` → item in-app; e-mail AFTER_COMMIT (`NotificationEventFlowIT`) |
| AC2 (2 não lidas → marcar → contador 0) | Frontend unit | `NotificationsStateService`/sino + central: contador reativo; marcar uma/todas |
| AC3 (opt-out não-obrigatório suprime e-mail, mantém in-app) | Integração | `NotificationEventFlowIT.optOutOfNonMandatoryType_keepsInAppItem_butSendsNoEmail` |
| AC4 (tipo obrigatório não desativa e-mail → recusa) | Domínio + API + FE | `Notifications.updatePreferences` (422 `MandatoryPreferenceOptOutException`); tela de preferências trava tipos mandatory |
| AC5 (sem dado sensível no conteúdo) | Domínio (guarda de conteúdo) | Templates pt-BR sem CPF/CNS/bancário (BR4) |

### SPEC-0006 — Perfil e Conta

| AC | Como foi verificado | Evidência |
|---|---|---|
| AC1 (formato de celular inválido → erro no campo, bloqueia) | FE unit + API | validadores de contato; `422 profile.mobile-invalid` |
| AC2 (novo e-mail de contato salvo + auditado) | Integração | `ProfileApiIT` (persistência parcial + linha de auditoria + evento `ContactDataChanged`) |
| AC3 (executável renomeado .png recusado) | Domínio + Integração | sniff de magic bytes; `422 profile.photo-invalid-content` |
| AC4 (titular altera foto do dependente; dependente não altera contatos do titular) | Integração (escopo SPEC-0003) | `ProfileApiIT` negação de escopo PEDRO→MARIA |
| AC5 (nova versão de Termos → interceptação até "Li e aceito") | FE unit + Integração | guarda `CanActivateChild` + `LegalDocumentApiIT` (versionamento/aceite; 409 `legal.version-outdated`) |
| AC6 (Sair encerra a sessão) | FE unit + jornada E2E | confirmação + logout; jornada em `e2e/perfil.spec.ts` (autorada) |
| AC7 (limpar celular → recusa obrigatória) | Integração | `422 profile.mobile-required` |

### SPEC-0007 — Carteirinha Digital

| AC | Como foi verificado | Evidência |
|---|---|---|
| AC1 (dados da MARIA: nome, cartão, CNS, ANS, abrangência, adicional) | Integração | `CardApiIT` (AC1 completo sobre o seed real) |
| AC2 (Salvar Carteirinha → PDF com campos BR3) | Domínio | `CardPdfRendererTest` (campos extraídos do PDF via `PdfTextExtractor`) |
| AC3 (selecionar PEDRO → cartão dele + auditoria do acesso do titular) | Integração + FE | `CardApiIT` (linha de auditoria dependente); recarga na troca do seletor |
| AC4 (PEDRO autenticado → só a carteirinha dele) | Integração (escopo) | `CardApiIT` escopo familiar |
| AC5 (copiar número → 9 dígitos + confirmação) | FE unit | ação de copiar (conteúdo exato) |
| AC6 (beneficiário inativo → estado indisponível) | Integração + FE | `409 card.unavailable`; estado dedicado |

**Nota E2E:** as jornadas E2E (Playwright) de carteirinha, notificações e perfil foram **autoradas**
pelos devs; a execução sobre o stack montado é o passo final da integração/CI (as camadas
unit/integração e FE unit que sustentam cada AC já estão verdes).

## Extras (mesmo PR)

- **CodeQL — armazenamento em texto claro (ADR-0009):** o número da carteirinha saiu do claim JWT
  `beneficiary_card`; agora é resolvido no servidor (`SecurityContextUserProvider` via
  `IdentityAccounts.beneficiaryCardFor`), sem mudar os consumidores. Regressão real
  (`LoginAndTokenFlowIT`): o token emitido não carrega o cartão — **falha antes, passa depois**
  (confirmado por execução).
- **`AuditRetentionJob` → `application`:** um job é mecanismo de entrega (como um endpoint REST),
  então saiu de `infra` para `application.jobs`, chamando a facade pública `domain.audit`; ArchUnit
  e Modulith seguem verdes (`SchedulingConfig`/`@EnableScheduling` permanece em `infra`).

## Governança

- **Decisões autônomas (run autorizado):** DL-0006 (destinatário sem conta = titular), DL-0007
  (retenção in-app 12 meses), DL-0008 (escopo de centralização de e-mail), DL-0009 (PDF em A4),
  DL-0010 (`planCategory` seed), DL-0011 (placement de perfil/legal).
- **ADRs:** 0007 (OpenPDF), 0008 (módulo `domain.notification`), 0009 (de-sensibilização do JWT),
  0010 (módulo `domain.card`). Mapa de módulos agora com **sete** módulos verificados.

## Qualidade

- Backend `./mvnw verify`: **321 testes**, 0 falhas; Checkstyle 0; coverage nos pisos; ArchUnit +
  Spring Modulith `verify()`; snapshots OpenAPI e diagrama de módulos (comparação de diagrama
  **tornada determinística** — ordenação das linhas `Rel`, pois o `Documenter` do Modulith as emite
  fora de ordem; bug de gate flaky corrigido no caminho).
- Frontend `lint`/`test`/`build`: **204 testes**, verdes; cobertura acima dos pisos.
- Migrations V9 (card), V10 (notificações), V11/V12 (perfil/legal). OpenAPI e diagrama regenerados.

## Retrospectiva do workflow

**Linha do tempo (handoffs):**

- **Governança primeiro (lição registrada):** specs 0004/0006/0007 aprovadas, Open Questions
  resolvidas (DLs) e ADRs criados **antes** de ramificar — mas o commit de governança entrou em
  `feature/phase-2` **depois** dos sub-branches, causando colisão de numeração DL-0006/ADR-0007 com
  o que o card-be criou; resolvido renumerando os artefatos do card para 0010 na integração.
- **Onda 1 (paralela):** Carteirinha (BE sonnet ∥ FE sonnet) + Notificações (BE opus ∥ FE sonnet) —
  4 agentes, worktrees isolados, landing-check confirmando main limpo. Fix CodeQL delegado após a
  Onda 1 (dev-backend opus). Integrados em `feature/phase-2` com renumeração de governança, fix do
  gate flaky do diagrama e regeneração de snapshots; verify limpo verde.
- **Onda 2 (paralela):** Perfil (BE opus ∥ FE opus), ramificada do tip já integrado (55338d0) — o
  que **evitou conflitos cross-branch de frontend** (shell/rotas/i18n), que só o segundo merge da
  Onda 1 sofreu.
- **Cross-spec:** `ContactDataChanged` → notificação (in-app + e-mail antigo/novo) fiado inline pelo
  arquiteto na integração, com IT dedicada.

**Reworks / reconciliações de contrato (de integração, do arquiteto):**

1. Notificações — preferences: BE usa envelope `{preferences:[{type,...}]}` e PUT 200+catálogo; FE
   assumira array cru/`code`/void → re-sincronizado (dev notif-fe).
2. Perfil — 4 pontos: aceite legal com `{version}`, `body` legal via `GET /legal-documents/{type}`,
   código `profile.landline-invalid`, e **avatar via blob-fetch** (o `<img src>` não envia o Bearer)
   → re-sincronizado (dev profile-fe).
3. Gap de contrato pego por um dev antes de morrer no limite: faltava `GET` da foto → adicionado.

**Incidentes / gargalos:**

- **Limite de sessão da API (plano):** os dois agentes de Perfil (opus) caíram por limite no meio da
  Onda 2, ainda na leitura (nenhum trabalho perdido — `dirty=0`). Estratégia de recuperação
  conservadora: retomar o BE primeiro (testa a cota), depois o FE em paralelo; ambos **retomados por
  SendMessage** preservando o contexto de leitura. Lição: em ambiente com cota, spawns são o caminho
  caro — retomar > respawnar, e escalonar concorrência quando a cota aperta.
- **Gate flaky do diagrama de módulos:** o `domain.card` foi o primeiro módulo com relações
  suficientes para expor a ordem não-determinística das linhas `Rel` do `Documenter`; escalado por um
  dev em vez de contornado, corrigido tornando a comparação insensível à ordem (preserva 100% da
  detecção de drift).

**Lições aprendidas:**

- Commitar governança **antes** de ramificar (evita colisão de numeração DL/ADR).
- Ramificar a onda seguinte do **tip já integrado** elimina conflitos cross-branch (validado: a Onda
  2 mergeou limpa).
- Congelar o contrato com o **corpo/shape exatos** (não só o endpoint) reduz reconciliações — os
  pontos que reconciliamos foram justamente onde o corpo ficou implícito.

## Riscos e pendências

- **Execução dos E2E** sobre o stack montado (autorados; recomendado no CI/`/dod` final).
- **Bateria pesada de QA** (PIT/mutação + exploratório adversarial) não executada como agente
  dedicado nesta fase por restrição de cota; mitigado pelas suítes de integração Testcontainers
  abrangentes e pela revisão fresh-eyes.
- ADR-0009 (e demais ADRs) seguem **Proposed** — o owner aprova/flipa no merge.
- Atalho "Carteirinha" da Home segue desabilitado (escopo SPEC-0005) — retoque futuro, fora das 3
  specs desta entrega (decisão registrada).
