# Guia do fluxo Claude - FKMed Lean SDD

> Para o dono do projeto e para qualquer pessoa nova. A ideia é ganhar qualidade e
> produtividade sem transformar cada tarefa numa operação pesada de agentes, worktrees e
> handoffs.

## 1. A mudança principal

O fluxo antigo tentou simular uma equipe autônoma: arquiteto como gerente, developers em
worktrees, QA obrigatório, integração de sub-branches e relatório final versionado para cada
fatia. Isso gerou proteção em alguns incidentes, mas também aumentou coordenação, espera,
limpeza, conflitos e carga mental.

O novo fluxo é:

```text
Claude principal executa.
Architect ajuda a especificar e decidir arquitetura.
Reviewer revisa quando vale uma segunda leitura.
QA valida quando o risco justifica.
```

Ou seja: agente não é mão de obra paralela por padrão. Agente é instrumento de clareza ou
qualidade.

## 2. Os papéis

| Papel | Quando usar | O que não faz por padrão |
|---|---|---|
| **Claude principal** | 80% do trabalho: implementar slice, testar, atualizar docs, abrir PR | Não inventa regra de negócio; não mergeia PR |
| **Architect** | Criar/melhorar spec, ADR, modelagem, fronteira de módulo, decompor uma fase em slices | Não gerencia developers; não orquestra worktrees |
| **Reviewer** | Revisar diff/PR, achar bug, teste faltando, violação de arquitetura, risco de contrato | Não implementa correção sozinho |
| **QA** | Dinheiro, LGPD, autorização, auditoria/retenção, documento clínico, jobs, concorrência, integração externa, jornada ampla | Não é obrigatório em toda slice; não corrige código |

Não existe mais `developer` subagente. O desenvolvedor padrão é a conversa principal com o
Claude.

## 3. Fluxo de uma slice

```text
0 QUESTIONS -> 1 PLAN -> 2 TEST ANCHOR -> 3 IMPLEMENT -> 4 GATES
-> 5 REVIEW/QA IF RISK -> 6 DoD + PR
```

### 0. Questions

Leia a spec. Se houver dúvida que muda comportamento, contrato, dados, segurança ou
arquitetura, pergunte. Se o dono autorizou autonomia, registre a decisão em
`docs/decision-log/` antes de codar.

### 1. Plan

Plano curto, no chat:

- objetivo;
- spec(s);
- escopo e fora de escopo;
- critérios de aceite;
- ordem de implementação;
- teste-âncora;
- comandos de validação;
- docs que podem mudar;
- riscos/perguntas.

### 2. Test anchor

Antes ou no começo da implementação, defina uma evidência. Pode ser teste automatizado,
regressão que falha antes, chamada de API, comando reproduzível, E2E, screenshot ou
verificação manual. O ponto não é ritual: é não terminar uma slice sem prova.

### 3. Implement

O Claude principal implementa na branch da slice, seguindo padrões existentes e mantendo a
spec viva.

### 4. Gates

Rode testes focados primeiro e depois gates proporcionais:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
cd frontend && npm run e2e:up && npm run e2e && npm run e2e:down
```

E2E é obrigatório quando uma jornada de usuário mudou. PIT/mutation fica para dinheiro ou
domínio crítico quando fizer sentido.

### 5. Review/QA if risk

Reviewer entra quando o diff tem peso. QA entra quando o risco justifica. Se não usar,
declare no fechamento: "Reviewer/QA não acionado porque a slice é pequena, sem risco de
contrato/dados/jornada".

### 6. DoD + PR

`/dod` fecha a fatia: critérios de aceite, evidências, Definition of Done, docs vivos,
status, push e PR para `develop`. O dono mergeia.

## 4. Worktrees

Worktree agora é exceção:

- spike arriscado;
- investigação longa;
- QA isolado;
- experimento paralelo aprovado;
- duas abordagens que precisam ser comparadas.

Para o trabalho normal, use a branch atual. Menos contexto duplicado, menos limpeza, menos
risco de escrever no lugar errado.

## 5. Relatórios e evidências

Os relatórios antigos em `docs/reports/final/` ficam como evidência histórica. Eles contam o
que aconteceu no fluxo anterior e não devem ser reescritos.

No novo fluxo:

- plano local em `docs/reports/plans/` é opcional;
- relatório final versionado é opcional;
- `docs/ROADMAP-STATUS.md` é o log normal de fechamento de slice;
- o PR e os testes são a principal evidência operacional.

Use relatório final quando a slice foi grande, crítica ou quando o dono pedir uma
retrospectiva.

## 6. Receitas rápidas

**Quero criar uma spec antes de codar**

```text
Use o Architect para transformar esta ideia numa spec implementável:
<ideia>
Faça perguntas só sobre o que muda comportamento, contrato, dados, segurança ou arquitetura.
No fim, crie/atualize a spec e proponha as próximas slices pequenas.
```

**Quero implementar uma slice**

```text
Execute a próxima slice usando o FKMed Lean SDD:
- leia a spec <SPEC-NNNN>
- resolva Open Questions comigo antes de codar
- faça plano curto com critérios de aceite
- defina um teste-âncora
- implemente na branch feature/<nome>
- rode gates proporcionais
- acione Reviewer/QA só se o risco justificar
- feche com /dod e PR para develop
```

**Quero revisar um PR**

```text
Use o Reviewer para revisar o PR <número> contra a spec, CLAUDE.md e arquitetura.
Traga achados por severidade, com arquivo/linha, risco, sugestão e veredito.
```

**Quero QA de risco**

```text
Use QA para validar esta slice contra a spec:
<slice/spec>
Foque em negativos, bordas, idempotência, LGPD/autorização/auditoria e evidências reais.
Não corrija código; reporte achados e risco residual.
```

## 7. O que nunca muda

- O Claude não inventa regra de negócio.
- Gate vermelho não é "contornado".
- Bug corrigido ganha regressão.
- Spec muda junto com o código que ela governa.
- Branch protegida é decisão humana: agente abre PR, dono mergeia.
