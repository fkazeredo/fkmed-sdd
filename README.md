# FKMed - Portal do Beneficiario e POC de Spec-Driven Development

**FKMed** e uma POC de portal de beneficiario de plano de saude construido com Spec-Driven
Development e Claude Code: carteirinha digital, primeiro acesso, contexto familiar,
notificacoes, rede credenciada, agendamentos, telemedicina, documentos clinicos, guias,
token de atendimento e financeiro do contrato.

Este repositorio tambem conta uma historia de metodo. O projeto avancou rapido com specs,
ADRs, decision logs e gates fortes; depois tentou um fluxo pesado de agentes autonomos,
worktrees e integracao por sub-branches; e agora migrou para **FKMed Lean SDD**: Claude
principal executa, Architect ajuda a especificar, Reviewer revisa e QA entra quando o risco
justifica.

Nada foi escondido: os relatorios antigos em `docs/reports/final/` ficam como evidencia do
fluxo anterior, inclusive dos gargalos e incidentes que motivaram a mudanca.

---

## Sumario

- [1. O que e o sistema](#1-o-que-e-o-sistema)
- [2. Resultado em numeros](#2-resultado-em-numeros)
- [3. O metodo atual: FKMed Lean SDD](#3-o-metodo-atual-fkmed-lean-sdd)
- [4. A historia ate agora](#4-a-historia-ate-agora)
- [5. O que aprendemos sobre agentes](#5-o-que-aprendemos-sobre-agentes)
- [6. Stack](#6-stack)
- [7. Como rodar](#7-como-rodar)
- [8. Mapa do repositorio](#8-mapa-do-repositorio)

---

## 1. O que e o sistema

O FKMed simula um portal realista de operadora/plano de saude para beneficiarios:

- primeiro acesso, verificacao de e-mail, login OIDC, recuperacao de senha e seguranca de
  conta;
- titular e dependentes, seletor de beneficiario ativo e autorizacao server-side por escopo;
- home, notificacoes, perfil, documentos legais, foto e dados cadastrais;
- carteirinha digital com PDF;
- busca de rede credenciada e agendamento/cancelamento/remarcacao de consultas e exames;
- telemedicina com fila de pronto atendimento, SSE e sala state-driven;
- Minha Saude com prescricoes, atestados, pedidos de exame e encaminhamentos;
- guias, token antifraude de atendimento e API de simulacao de operador para jornadas
  demonstraveis;
- financeiro: boletos, linha digitavel, PIX, segunda via PDF, validador antifraude,
  coparticipacao, informe de IR e declaracao de quitacao.

A proxima fase planejada no roadmap e **Reembolso**: solicitacao, analise automatica,
pendencias, aprovacao, pagamento, glosa e demonstrativo.

## 2. Resultado em numeros

Estado documentado mais recente: **v0.10.0, 2026-07-06**, Fase 5 slice 5.2
(`docs/ROADMAP-STATUS.md` e `docs/release-notes/CHANGELOG.md`).

| Metrica | Valor |
|---|---:|
| Commits no historico local | 170 |
| Specs de produto | 18 |
| ADRs | 20 |
| Decision logs | 22 |
| Migracoes Flyway | 24 |
| Arquivos Java em `backend/src/main/java` | 396 |
| Arquivos TS/HTML/SCSS em `frontend/src` | 232 |
| Ultima bateria backend registrada | 710 testes |
| Ultima bateria frontend registrada | 507 testes |
| Ultima E2E registrada | 30 jornadas Playwright |

Esses numeros sao deliberadamente conservadores: saem do repo e dos logs versionados, nao de
estimativa.

## 3. O metodo atual: FKMed Lean SDD

A tese continua a mesma: **a IA nao inventa o produto; ela executa specs sob regras e gates**.
O que mudou foi o tamanho do processo.

O fluxo atual esta em [CLAUDE.md](CLAUDE.md), [AGENTS.md](AGENTS.md),
[docs/TUTORIAL.md](docs/TUTORIAL.md), [docs/architecture/workflow.md](docs/architecture/workflow.md),
[docs/GUIA-TIME-CLAUDE.md](docs/GUIA-TIME-CLAUDE.md) e
[docs/GUIA-CODEX.md](docs/GUIA-CODEX.md):

```text
0 QUESTIONS -> 1 PLAN -> 2 TEST ANCHOR -> 3 IMPLEMENT -> 4 GATES
-> 5 REVIEW/QA IF RISK -> 6 DoD + PR
```

As regras praticas:

- o Claude principal e o executor padrao;
- `architect` cria/melhora spec, ADR e desenho de slice;
- `reviewer` faz segunda leitura tecnica de diff/PR;
- `qa` valida slices de risco;
- worktree e excecao, nao padrao;
- relatorio final versionado e opcional, nao imposto a toda slice;
- specs, ADRs, decision logs, manual, changelog e roadmap-status continuam vivos.

## 4. A historia ate agora

### Fase 0 - Walking skeleton

O projeto colocou de pe o caminho inteiro: backend Spring Boot, banco PostgreSQL/Flyway,
Authorization Server embutido, Angular, Docker Compose, CI, gates de arquitetura e a tela
inicial **Meu Plano** com massa da familia MARIA/PEDRO.

### Fase 1 - Entrar no portal

O login deixou de ser seam em memoria e virou conta real em banco. Entraram primeiro acesso,
verificacao por e-mail, lockout, recuperacao de senha, troca de senha, sessoes, contexto de
beneficiario ativo, autorizacao por escopo familiar e a Home.

Um incidente importante apareceu aqui: um subagente escreveu no worktree principal em vez do
worktree isolado. O trabalho foi resgatado, mas a licao ficou registrada e influenciou o
fluxo pesado posterior.

### Fase 2 - Minha conta e identificacao

Entraram central de notificacoes, preferencias de e-mail, perfil, contatos, foto, documentos
legais versionados, aceite obrigatorio, carteirinha digital e PDF. Tambem houve hardening:
o numero da carteirinha saiu do JWT, e o `AuditRetentionJob` foi movido para a camada
correta de aplicacao/delivery.

### Fase 3 - Encontrar atendimento

Entraram busca de rede credenciada com geografia IBGE, cobertura por plano, busca por nome,
detalhe de prestador, agendamento de consultas e exames, upload de pedido medico, capacidade
real de agenda sob concorrencia e protocolos `AG-*`.

O CI/E2E ajudou a encontrar defeitos reais de contrato e tela, e a fase fechou com Playwright
verde registrado.

### Fase 4 - Cuidado digital

Entraram telemedicina, fila de pronto atendimento, primeiro uso de SSE, sala state-driven
sem midia, teleconsulta agendada, documentos clinicos imutaveis, PDFs e simulacao de
operador para dirigir a jornada completa.

Foi uma fase produtiva, mas tambem cara em coordenacao: varios agentes paralelos, ondas,
reconciliacoes e E2E que nao deveria ter esperado o CI para provar o fluxo.

### Fase 5 - Plano e financas

Entraram guias, token de atendimento, simulacao de operador para guias, financeiro do
contrato, boletos, PIX, linha digitavel, segunda via, validador antifraude,
coparticipacao, IR e declaracao de quitacao.

A slice 5.1 mostrou uma falha tipica do processo pesado: contrato congelado em plano
gitignored nao chegou ao worktree de frontend, causando rework. A slice 5.2 melhorou ao usar
um fluxo mais sequencial, end-to-end, com menos split de contrato.

## 5. O que aprendemos sobre agentes

O FKMed nao falhou por falta de rigor. Ele falhou quando o rigor virou custo fixo demais:
worktree para tudo, varios devs autonomos, handoff demais, relatorio demais e integracao
demais para tarefas que cabiam num fluxo direto.

O aprendizado atual:

- **agente principal ja e dev**: criar um `developer` subagente padrao duplica contexto;
- **Architect e excelente para spec**: ajuda a transformar ideia vaga em regra testavel;
- **Reviewer vale muito**: segunda leitura pega teste faltando, contrato quebrado e
  overengineering;
- **QA vale quando ha risco real**: dinheiro, LGPD, autorizacao, auditoria, documentos
  clinicos, jobs, concorrencia, integracoes e jornadas amplas;
- **worktree e ferramenta cirurgica**: boa para isolamento, ruim como reflexo automatico;
- **evidencia fica**: os relatorios antigos continuam em `docs/reports/final/`, mas nao sao
  obrigatorios para cada nova slice.

## 6. Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Modulith, Spring Security, Spring Authorization Server |
| Banco | PostgreSQL 16, Flyway |
| Frontend | Angular 22, PrimeNG, Tailwind 4, ngx-translate |
| Testes backend | JUnit 5, Testcontainers, ArchUnit, jqwik, PIT, JaCoCo |
| Testes frontend | Vitest, Playwright |
| Qualidade | Spotless, Checkstyle, OpenAPI snapshot, Modulith diagram, i18n/error completeness |
| Infra local | Docker Compose, Mailpit, Grafana/observabilidade conforme compose |
| Governanca | GitHub Actions, gitleaks, pre-commit, branch protection documentada |

## 7. Como rodar

Prerequisitos: Docker, Node.js LTS e JDK 21.

```bash
docker compose up -d
cd frontend && npm ci && npm start
```

URLs principais:

- app: http://localhost:4200
- backend/API/OIDC: http://localhost:8080
- Grafana: http://localhost:3000

Conta dev principal:

```text
maria@fkmed.local / maria12345
```

Comandos de verificacao:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
```

PIT/mutation e usado quando a slice toca dinheiro ou dominio critico:

```bash
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage
```

## 8. Mapa do repositorio

| Caminho | Conteudo |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Constituicao operacional do projeto/Claude Code |
| [AGENTS.md](AGENTS.md) | Instrucao canonica para Codex neste repo |
| [.claude/agents/](.claude/agents/) | Agentes atuais: architect, reviewer, qa |
| [.claude/skills/](.claude/skills/) | Comandos `/spec`, `/slice`, `/dod`, `/manual`, `/ci-triage` etc. |
| [.agents/skills/](.agents/skills/) | Skills repo-locais do Codex |
| [docs/GUIA-CODEX.md](docs/GUIA-CODEX.md) | Como alternar entre Claude Code e Codex |
| [docs/specs/](docs/specs/) | Specs vivas do produto |
| [docs/adr/](docs/adr/) | Decisoes de arquitetura |
| [docs/decision-log/](docs/decision-log/) | Decisoes autonomas autorizadas |
| [docs/architecture/](docs/architecture/) | Regras detalhadas de arquitetura |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Fases planejadas |
| [docs/ROADMAP-STATUS.md](docs/ROADMAP-STATUS.md) | Log de execucao por slice |
| [docs/reports/](docs/reports/) | Evidencias e retrospectivas historicas/opcionais |
| [docs/MANUAL.md](docs/MANUAL.md) | Manual do usuario |
| [docs/release-notes/CHANGELOG.md](docs/release-notes/CHANGELOG.md) | Changelog |
| [backend/](backend/) | Backend Java/Spring |
| [frontend/](frontend/) | Frontend Angular |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Fluxo de PR e gates |
| [SECURITY.md](SECURITY.md) | Politica de seguranca e credenciais dev-only |

Este projeto e um estudo vivo. O objetivo nao e provar que "mais agentes" e sempre melhor;
e descobrir qual e o menor fluxo que ainda entrega software verificavel, com qualidade e sem
travar o dono do produto.
