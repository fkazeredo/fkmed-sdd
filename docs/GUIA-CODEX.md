# Guia Codex - como alternar entre Claude Code e Codex

Este projeto pode ser trabalhado por Claude Code ou Codex sem mudar a fonte de verdade do
produto. A diferenca fica nos arquivos de entrada de cada ferramenta.

## Mapa rapido

| Uso | Claude Code | Codex |
|---|---|---|
| Instrucao canonica da ferramenta | `CLAUDE.md` | `AGENTS.md` |
| Skills/agentes especificos | `.claude/` | `.agents/skills/` |
| Fonte de produto/arquitetura | `docs/specs`, `docs/adr`, `docs/architecture`, roadmap/status | os mesmos |
| Fluxo padrao | FKMed Lean SDD | FKMed Lean SDD |

## Regra de interoperabilidade

`CLAUDE.md` continua sendo a constituicao do projeto. Para o Codex, `AGENTS.md` e a porta
canonica que manda ler e respeitar `CLAUDE.md`, salvo conflito com instrucoes superiores do
proprio Codex.

Assim, o projeto fica com duas camadas:

```text
Camada compartilhada:
CLAUDE.md + docs/specs + docs/adr + docs/architecture + roadmap/status

Camada Claude Code:
.claude/agents + .claude/skills + settings Claude

Camada Codex:
AGENTS.md + .agents/skills
```

## Como pedir uma slice ao Codex

```text
Use $fkmed-lean-sdd.

Entrada principal:
docs/plan/<arquivo-da-slice>.md

Antes de implementar:
- leia AGENTS.md, CLAUDE.md e docs/architecture/workflow.md
- leia specs/ADRs citados no plano
- identifique Open Questions, conflitos ou lacunas

Se estiver claro:
- faca plano curto
- defina o teste-ancora
- implemente a slice
- rode gates proporcionais
- atualize docs vivos
- no final me diga arquivos alterados, testes rodados e riscos
```

## Como pedir somente spec/desenho

```text
Use AGENTS.md e CLAUDE.md como regras do projeto.
Leia o contexto abaixo e me ajude a transformar em spec implementavel.
Nao implemente ainda.
```

## Como voltar para Claude Code

No Claude Code, continue usando `CLAUDE.md` e os arquivos em `.claude/`. Nao e necessario
apagar `AGENTS.md` nem `.agents/skills`; eles existem para o Codex.

## O que nao duplicar

Nao replique specs, ADRs ou decisoes entre ferramentas. A documentacao de produto deve
continuar em `docs/`. As pastas `.claude/` e `.agents/` devem conter apenas a mecanica de
cada agente.
