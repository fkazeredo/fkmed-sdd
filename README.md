# FKMed - Portal do Beneficiario e POC de Spec-Driven Development

**FKMed** e uma POC de portal de beneficiario de plano de saude construido com
Spec-Driven Development: primeiro acesso, login OIDC, contexto familiar, notificacoes,
perfil, carteirinha digital, rede credenciada, agendamentos, telemedicina, documentos
clinicos, guias, token de atendimento, financeiro do contrato, atendimento/FAQ e
reembolso.

O repositorio tambem documenta o metodo de trabalho. O projeto comecou com um fluxo pesado
de agentes, worktrees e relatorios longos; hoje usa **FKMed Lean SDD**: executor principal,
specs vivas, decision logs quando ha autonomia, gates fortes, reviewer/QA quando o risco
justifica.

---

## Sumario

- [1. O que e o sistema](#1-o-que-e-o-sistema)
- [2. Estado atual em numeros](#2-estado-atual-em-numeros)
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
- Home, notificacoes, perfil, documentos legais, foto e dados cadastrais;
- carteirinha digital com PDF;
- busca de rede credenciada e agendamento/cancelamento/remarcacao de consultas e exames;
- telemedicina com fila de pronto atendimento, SSE e sala state-driven;
- Minha Saude com prescricoes, atestados, pedidos de exame e encaminhamentos;
- guias, token antifraude de atendimento e API de simulacao de operador para jornadas
  demonstraveis;
- financeiro: boletos, linha digitavel, PIX, segunda via PDF, validador antifraude,
  coparticipacao, informe de IR e declaracao de quitacao;
- atendimento: canais oficiais, antifraude, FAQ e solicitacao de Libras;
- reembolso: solicitacao, analise automatica, pendencias, aprovacao/negativa, glosa,
  pagamento, extrato e previas.

O POC planejado ate a Fase 6 esta implementado. Os proximos passos naturais sao hardening,
backoffice real, privacidade operacional, acessibilidade automatizada, performance e
preparacao de producao.

## 2. Estado atual em numeros

Estado documentado mais recente: **v0.13.0, 2026-07-09**, Fase 6 concluida e hardening
pos-fase com armazenamento de arquivos configuravel. A fatia tambem atualiza gates,
observabilidade, limites de upload e documentacao operacional.

| Metrica | Valor |
|---|---:|
| Commits no historico local antes desta fatia | 186 |
| Specs de produto | 19 |
| ADRs de projeto | 23 |
| Decision logs | 35 |
| Migracoes Flyway | 28 |
| Arquivos Java em `backend/src/main/java` | 515 |
| Arquivos TS/HTML/SCSS/CSS em `frontend/src` | 250 |
| Ultima bateria backend local registrada | 786 testes |
| Ultima bateria frontend local registrada | 524 testes |
| Ultima bateria E2E local registrada | 33/33 jornadas Playwright |
| Caderno de QA humano | 42 jornadas guiadas + 241 casos atomicos |

Os numeros acima saem do repositorio e dos logs versionados. A contagem exata de testes
sempre deve ser confirmada pela ultima execucao dos gates.

## 3. O metodo atual: FKMed Lean SDD

A tese continua a mesma: **a IA nao inventa o produto; ela executa specs sob regras e gates**.

O fluxo atual esta em [CLAUDE.md](CLAUDE.md), [AGENTS.md](AGENTS.md),
[docs/architecture/workflow.md](docs/architecture/workflow.md), [docs/GUIA-CODEX.md](docs/GUIA-CODEX.md)
e nos guias de arquitetura:

```text
0 QUESTIONS -> 1 PLAN -> 2 TEST ANCHOR -> 3 IMPLEMENT -> 4 GATES
-> 5 REVIEW/QA IF RISK -> 6 DoD + PR
```

Regras praticas:

- o executor principal implementa a fatia;
- `architect` ajuda em spec/ADR/desenho quando necessario;
- `reviewer` faz segunda leitura tecnica quando o diff ou risco pede;
- `qa` valida dinheiro, LGPD, autorizacao, auditoria, documentos clinicos, jobs,
  concorrencia, integracoes e jornadas amplas;
- worktree e excecao, nao reflexo automatico;
- uma fatia autorizada e verde termina com commit, push da feature branch e PR para `develop`, sem
  uma segunda confirmacao; `local-only`/`no PR` nao publica e `draft` abre um Draft PR;
- specs, ADRs, decision logs, manual, caderno de QA, changelog e roadmap-status continuam
  vivos.

## 4. A historia ate agora

### Fase 0 - Walking skeleton

Backend Spring Boot, PostgreSQL/Flyway, Authorization Server embutido, Angular, Docker
Compose, CI, gates de arquitetura e a tela inicial **Meu Plano**.

### Fase 1 - Entrar no portal

O login deixou de ser seam em memoria e virou conta real em banco. Entraram primeiro acesso,
verificacao por e-mail, lockout, recuperacao de senha, troca de senha, sessoes, contexto de
beneficiario ativo, autorizacao por escopo familiar e Home.

### Fase 2 - Minha conta e identificacao

Entraram central de notificacoes, preferencias, perfil, contatos, foto, documentos legais
versionados, aceite obrigatorio, carteirinha digital e PDF. Tambem houve hardening: o
numero da carteirinha saiu do JWT.

### Fase 3 - Encontrar atendimento

Entraram rede credenciada, geografia IBGE, cobertura por plano, busca por nome, detalhe de
prestador, agendamento de consultas/exames, upload de pedido medico, capacidade real sob
concorrencia e protocolos `AG-*`.

### Fase 4 - Cuidado digital

Entraram telemedicina, fila de pronto atendimento, SSE, sala state-driven sem midia,
teleconsulta agendada, documentos clinicos imutaveis, PDFs e simulacao de operador para a
jornada completa.

### Fase 5 - Plano e financas

Entraram guias, token de atendimento, financeiro, boletos, PIX, linha digitavel, segunda via,
validador antifraude, coparticipacao, IR, declaracao de quitacao, canais de atendimento,
FAQ, antifraude e Libras.

### Fase 6 - Reembolso

Entraram solicitacao de reembolso com termo, uploads, protocolo, analise automatica,
historico, detalhe, timeline, pendencias, correcao bancaria, aprovacao/negativa/pagamento,
glosa, extrato de pagos, previas e acoes de operator-sim.

## 5. O que aprendemos sobre agentes

O FKMed nao falhou por falta de rigor. Ele ficou caro quando o rigor virou custo fixo:
worktree para tudo, muitos handoffs, contratos congelados fora do repo e relatorios demais.

Aprendizado atual:

- agente principal ja e dev;
- Architect e mais valioso antes de codar, na spec;
- Reviewer vale quando ha diffs amplos ou risco;
- QA vale quando ha risco real;
- worktree e ferramenta cirurgica;
- evidencias antigas ficam em `docs/reports/final/`, mas nao sao obrigatorias por fatia.

## 6. Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Modulith, Spring Security, Spring Authorization Server |
| Banco | PostgreSQL 16, Flyway |
| Arquivos | Adapter configuravel: PostgreSQL binario, filesystem local ou Amazon S3 |
| Frontend | Angular 22, PrimeNG, Tailwind 4, ngx-translate |
| Testes backend | JUnit 5, Testcontainers, ArchUnit, Spring Modulith verify, PIT, JaCoCo |
| Testes frontend | Vitest, Playwright |
| Qualidade | Spotless, Checkstyle, OpenAPI snapshot, module diagram, i18n/error completeness |
| Infra local | Docker Compose, Mailpit, Grafana/Prometheus conforme compose |
| Governanca | GitHub Actions, Gitleaks, branch protection documentada |

## 7. Como rodar

Prerequisitos: Docker, Node.js/npm compativel com `frontend/package.json` e JDK 21.

```bash
docker compose up -d
cd frontend && npm ci && npm start
```

Uploads no stack de desenvolvimento ficam no volume Docker montado em `/fkmed/uploads`. O destino
e configuravel no `.env`:

```dotenv
FKMED_STORAGE_BACKEND=filesystem
FKMED_STORAGE_FILESYSTEM_ROOT=/fkmed/uploads
```

Valores aceitos: `postgres`, `filesystem` e `s3`. O profile `prod` usa S3 e exige
`FKMED_STORAGE_S3_BUCKET` e `AWS_REGION`; consulte `.env.example` e ADR-0023.

URLs principais:

- app: http://localhost:4200
- backend/API/OIDC: http://localhost:8080
- Grafana: http://localhost:3000

Conta dev principal:

```text
maria@fkmed.local / maria12345
```

Conta dev-only para operator-sim:

```text
operador-sim@fkmed.local / operador12345
```

Comandos de verificacao:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
```

PIT/mutation e usado quando a fatia toca dinheiro ou dominio critico:

```bash
cd backend && ./mvnw -Pmutation org.pitest:pitest-maven:mutationCoverage
```

## 8. Mapa do repositorio

| Caminho | Conteudo |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Constituicao operacional do projeto/Claude Code |
| [AGENTS.md](AGENTS.md) | Instrucao canonica para Codex neste repo |
| [.claude/agents/](.claude/agents/) | Agentes atuais: architect, reviewer, qa |
| [.claude/skills/](.claude/skills/) | Comandos `/spec`, `/slice`, `/dod`, `/manual`, `/ci-triage` |
| [.agents/skills/](.agents/skills/) | Skills repo-locais do Codex |
| [docs/README.md](docs/README.md) | Indice da documentacao |
| [docs/GUIA-CODEX.md](docs/GUIA-CODEX.md) | Como alternar entre Claude Code e Codex |
| [docs/specs/](docs/specs/) | Specs vivas do produto |
| [docs/adr/](docs/adr/) | Decisoes de arquitetura |
| [docs/decision-log/](docs/decision-log/) | Decisoes autonomas autorizadas |
| [docs/architecture/](docs/architecture/) | Regras detalhadas de arquitetura |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Fases planejadas |
| [docs/ROADMAP-STATUS.md](docs/ROADMAP-STATUS.md) | Log de execucao por fatia |
| [docs/MANUAL.md](docs/MANUAL.md) | Manual do usuario |
| [docs/QA-CADERNO-DE-TESTES.md](docs/QA-CADERNO-DE-TESTES.md) | Caderno de testes para QA humano |
| [docs/release-notes/CHANGELOG.md](docs/release-notes/CHANGELOG.md) | Changelog |
| [backend/](backend/) | Backend Java/Spring |
| [frontend/](frontend/) | Frontend Angular |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Fluxo de PR e gates |
| [SECURITY.md](SECURITY.md) | Politica de seguranca e credenciais dev-only |

Este projeto e um estudo vivo: o objetivo e descobrir o menor fluxo que ainda entrega
software verificavel, com qualidade, rastreabilidade e sem deixar o dono do produto as cegas.
