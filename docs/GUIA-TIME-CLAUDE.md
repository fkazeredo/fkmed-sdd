# Guia do time de agentes — como usar o fluxo (do zero)

> Para o dono e para qualquer pessoa nova no projeto. **Não assume nenhum conhecimento de
> Claude Code nem de agentes.** Em 15 minutos de leitura você sabe operar o time inteiro.

## Índice

1. [Os 3 conceitos que você precisa (2 minutos)](#1-os-3-conceitos-que-você-precisa-2-minutos)
2. [A regra de ouro: você só fala com o arquiteto](#2-a-regra-de-ouro-você-só-fala-com-o-arquiteto)
3. [Os 10 comandos (skills)](#3-os-10-comandos-skills)
4. [O time: arquiteto, devs e QA](#4-o-time-arquiteto-devs-e-qa)
5. [O fluxo de uma fatia, ponta a ponta](#5-o-fluxo-de-uma-fatia-ponta-a-ponta)
6. [Receitas rápidas do dia a dia](#6-receitas-rápidas-do-dia-a-dia)
7. [O vai-e-volta (rework)](#7-o-vai-e-volta-rework)
8. [O que os agentes NUNCA fazem (e por quê)](#8-o-que-os-agentes-nunca-fazem-e-por-quê)
9. [Criar um projeto novo a partir deste](#9-criar-um-projeto-novo-a-partir-deste)
10. [Perguntas frequentes](#10-perguntas-frequentes)

---

## 1. Os 3 conceitos que você precisa (2 minutos)

**Claude Code** é o assistente que roda no seu terminal (ou no VS Code). Você conversa com
ele em português, ele lê e escreve código, roda comandos e testes. Tudo o que ele faz aparece
na tela e pede sua permissão quando é sensível.

**Skill** (ou "comando de barra") é uma **receita pronta**. Você digita `/` + o nome — por
exemplo `/dev-env` — e o assistente executa aquele procedimento do jeito certo, sempre igual.
Pense num skill como um **checklist que executa a si mesmo**. As receitas deste projeto ficam
na pasta `.claude/skills/` e viajam com o repositório: todo mundo que clonar o projeto tem os
mesmos comandos.

**Agente** é um **funcionário especializado** que trabalha **separado da sua conversa** (não
polui o seu chat com o trabalho braçal; volta só com o resultado). Os agentes deste projeto
ficam em `.claude/agents/`.

> Resumo: **skill = receita que se invoca com `/`; agente = funcionário que trabalha em
> segundo plano.**

## 2. A regra de ouro: você só fala com o arquiteto

Neste time, **o arquiteto é o seu único interlocutor**. Você não precisa saber qual
"funcionário" faz o quê — pede tudo a ele:

| Você quer… | Você diz ao arquiteto… |
|---|---|
| Especificar uma ou várias features | "cria uma spec para X" / "cria specs para X, Y e Z" |
| Melhorar uma spec existente | "melhora a SPEC-0012" |
| Registrar uma decisão de arquitetura | "registra um ADR para essa decisão" |
| Implementar | "implementa a SPEC-0035" |
| Revisar um PR antes de você mergear | "revisa o PR 16" / "resume o PR 16 pra mim" |
| Saber o status | "relatório da fase atual" / "resumo executivo do mês" |

O arquiteto **é também o documentador, o revisor e o relator** — essas funções são dele. Para
revisão de PR, ele usa por dentro um "par de olhos frescos" (um ajudante descartável que lê o
código friamente, sem apego ao que foi feito) e te entrega o briefing com a opinião dele em
cima. Você não precisa acionar nada disso — é mecânica interna.

**E a regra número 1 dele: nunca inferir.** Se faltar informação, ele para e **pergunta a
você** — mesmo no meio do trabalho. Ele só decide sozinho se você disser explicitamente "pode
decidir", e aí cada decisão fica registrada em `docs/decision-log/` para você auditar.

## 3. Os 10 comandos (skills)

Digite `/` no Claude Code para ver a lista. Os deste projeto:

| Comando | Para quê | Quando usar |
|---|---|---|
| `/spec` | Cria uma especificação nova a partir do template oficial | Antes de qualquer feature nova |
| `/adr` | Registra uma decisão de arquitetura | Quando uma decisão muda estrutura/stack |
| `/dl` | Registra uma decisão tomada em autonomia | Só em execução autônoma autorizada |
| `/slice` | Abre uma fatia: valida a spec, cria a branch, monta o plano | Ao começar a implementar |
| `/dod` | Fecha a fatia: roda todos os testes/gates, confere o checklist, abre o PR | Quando a fatia parece pronta |
| `/release` | Sobe a versão em todos os lugares certos + changelogs pt/en | Fatia com código fechando |
| `/manual` | Atualiza o manual do usuário (pt + en, em sincronia) | Fatia que mudou algo visível ao usuário |
| `/dev-env` | Sobe o sistema completo na sua máquina para testar manualmente | "Quero ver funcionando" |
| `/ci-triage` | Diagnostica por que o CI (os testes do GitHub) ficou vermelho | PR com check vermelho |
| `/new-project` | Cria um produto NOVO a partir deste template | Raro; só manual |

Exemplos reais de uso (é só digitar assim, com argumentos em seguida):

```
/spec contas-a-receber gestão de recebíveis com baixa automática
/slice SPEC-0035 contas-a-receber
/dev-env
/ci-triage 15
```

Os skills são as **ferramentas** que o arquiteto usa para as funções dele — você pode
invocá-los direto, mas no dia a dia é mais simples pedir ao arquiteto.

## 4. O time: arquiteto, devs e QA

Três arquivos em `.claude/agents/`:

| Agente | Papel no time | Modelo / esforço |
|---|---|---|
| `architect` | **Seu único interlocutor.** Escreve specs e ADRs com você, planeja, distribui o trabalho, **integra e mergeia as branches dos developers (resolver conflito é habilidade dele)**, documenta, revisa (com olhos frescos), reporta, media o vai-e-volta. Nunca infere — pergunta. | **Opus em esforço `high`** (o ponto de equilíbrio; `xhigh` só em escalada deliberada — decisão cara de reverter, segurança, dinheiro, planejamento de fase complexa) |
| `developer` | Constrói a fatia **inteira, ponta a ponta** (Java/banco/APIs + Angular/telas): backend primeiro, frontend contra o contrato real. Escreve os testes de todas as camadas e **os roda ao final, antes de entregar ao QA** — TDD é opcional, a critério dele. Em cópia isolada do repositório. | Sonnet ou Opus — **o arquiteto decide** pela complexidade; esforço `high` |
| `qa` | **Primeiro homologa a entrega contra a SPEC** (aceitação da Sprint/fatia + exploratório que o developer não pensou) — achado vira rework para o developer, ou volta ao arquiteto se for complexo. **Fechada a homologação, roda a bateria completa** antes de liberar a entrega (gates sempre; E2E quando há jornada; PIT em domínio crítico) — **qualquer erro na bateria volta ao ARQUITETO** replanejar. Aprova ou reprova. Separado do developer de propósito: autor não audita o próprio trabalho. | **Sonnet em esforço `high`**; o arquiteto sobe para Opus em fatia crítica (segurança/dinheiro/LGPD/documento clínico) |

**A regra de delegação (sua):** **um `developer` ponta a ponta é o padrão** — backend
primeiro, frontend contra o contrato real, testes ao final. **Paralelismo é exceção, com
predileção por isolamento**: um segundo developer só entra quando há demanda separável de
verdade, de preferência em escopos que **não se esbarram** (módulos/arquivos disjuntos, sem
contrato compartilhado). Uma **sobreposição pequena é aceitável** — porque integrar é
habilidade do arquiteto: é ele quem mergeia as sub-branches
(`feature/<fatia>--<escopo>`) na branch da fatia, **resolve os conflitos** e valida a
integração. Ele escolhe o modelo de cada um (Sonnet para o rotineiro, Opus para o crítico) e
nunca fragmenta à toa nem deixa instância ociosa. Fatia pequena de verdade ele faz sozinho.
Você sempre sabe em qual branch cada um está, porque cada handoff anuncia a branch (ver §5).

**Fatia é o padrão; fase inteira é sua prerrogativa.** Se você pedir explicitamente uma fase
completa, o arquiteto **aceita** — não insiste em PRs menores nem pede confirmação de novo —
e organiza a fase internamente em ondas (contrato congelado → trabalho paralelo → integração
→ jornada/E2E → candidato a release), com **arquivos de "escritor único"** (rotas, i18n
global, snapshot da API, numeração de migrations…) que só o integrador toca, para os
developers paralelos não colidirem. **E os gates são proporcionais:** o developer escreve e
roda os testes e gates das stacks tocadas **uma vez, ao final**, antes do QA; a bateria
inteira roda uma vez na integração final e o QA fecha com a bateria completa **depois da
homologação**; o fechamento reaproveita evidência verde do mesmo commit em vez de rodar tudo
de novo. O E2E roda **verde localmente antes de qualquer PR** — o CI nunca é a primeira
execução dele (lição da Fase 4).

**Cada agente trabalha na própria cópia isolada (worktree), e quem orquestra isso é o
arquiteto.** Cada dev/QA recebe uma pasta de trabalho separada, para nunca pisar no trabalho
de outro nem no diretório principal. Garantir que a branch de cada um esteja livre antes de
começar — e manter o diretório principal fora do caminho — é responsabilidade do arquiteto.

**O arquiteto não deixa lixo na sua máquina.** Cada cópia isolada (worktree) e qualquer
arquivo temporário que o fluxo cria são removidos ao fechar a fatia — de verdade, não só
"desregistrados". No Windows, quando o caminho longo trava a remoção normal, ele usa um
método que dá conta; no fim, só as worktrees ativas ficam no disco.

**E a regra que vale para qualquer impedimento:** se um agente esbarra em algo que não é dele
resolver — não consegue assumir a branch, uma ferramenta/serviço indisponível, uma spec
ambígua, um teste que parece errado, uma tarefa maior que o combinado — ele **para e devolve
ao arquiteto**, nunca contorna, nunca inventa, nunca finge um resultado. O arquiteto resolve
(conserta o ambiente, libera a branch, esclarece com você, replaneja) e só então destrava. O
QA segue a mesma disciplina: julga contra a spec (não por preferência), aponta achados
concretos com evidência, nunca conserta o código ele mesmo, e **um achado fora do escopo da
fatia volta para o arquiteto analisar** — vira item de spec, fatia futura ou replanejamento,
nunca é descartado. (Você não precisa acionar nada disso — é mecânica interna; está aqui só
para transparência.)

## 5. O fluxo de uma fatia, ponta a ponta

**Passo 0 — abra a sessão como arquiteto** (no terminal, na pasta do projeto):

```
claude --agent architect
```

(O esforço padrão do projeto agora é `high` — o ponto de equilíbrio custo × inteligência.
*Ultracode* e `xhigh` **deixaram de ser o padrão**: a Fase 4 mostrou que esforço máximo em
tudo multiplica horas e tokens sem comprar qualidade onde não precisa. Se VOCÊ quiser força
máxima numa tarefa pontualmente difícil, ative na hora com `/effort xhigh` — o arquiteto não
vai mais sugerir isso por conta própria. E se esquecer do `--agent architect`, sem problema:
`claude` normal também funciona — o arquiteto é uma "persona" que deixa a coordenação mais
afiada, não um requisito.)

| Você diz… | O que acontece |
|---|---|
| "Quero uma tela de contas a receber com baixa automática" | O arquiteto conversa com você, pergunta (nunca supõe) e escreve a spec junto. As dúvidas que você não responder ficam registradas como *Open Questions* — nada é implementado por adivinhação. **Pode parar aqui se você só queria a spec.** |
| "Aprovado, pode implementar" | Ele abre a fatia (`/slice`): branch, plano — e te mostra o plano para aprovação. Depois delega ao `developer` (1 por padrão; mais de um só com escopos bem isolados), cada um em cópia isolada. |
| *(aguarde; ele reporta o progresso)* | O developer constrói livre (TDD opcional, a critério dele), escreve e roda os testes **ao final**, roda os gates das stacks tocadas e devolve um relatório. |
| "Roda o QA" (ou o arquiteto propõe) | O `qa` primeiro **homologa a entrega contra a spec** (aceitação da Sprint + exploratório) e, aprovada a homologação, roda a **bateria completa**. Veredito **APROVADO/REPROVADO** por estágio. |
| *(se reprovado)* | Achado de homologação volta **ao mesmo developer** (cada correção ganha um teste novo); achado complexo — ou **qualquer erro na bateria completa** — volta **ao arquiteto** para replanejar/resolver/delegar. Ver §7. |
| "Fecha a fatia" | `/dod`: gates de novo, checklist da Definition of Done, manual/changelog/versão em dia, e **abre o PR** para a `develop`. |
| "Revisa o PR pra mim" | O arquiteto (com os olhos frescos por dentro) te entrega o briefing: o que o PR faz, pontos críticos, cheiros, comentários prontos para copiar e um veredicto sugerido. |
| **Você mergeia** (no GitHub) | Essa parte é SÓ SUA. Nenhum agente mergeia, nunca. |

**Seus 4 portões:** spec → plano → merge → tag/release (este último só quando você pedir).

**Você nunca fica às cegas:** cada handoff entre os agentes aparece no seu chat, em
português, como diálogo de time — sempre com a branch à mostra e **com a data e a hora reais
do momento, SEMPRE convertidas para o seu fuso (São Paulo)**, independente do ambiente onde
o agente roda (o comando força o fuso: `TZ=America/Sao_Paulo date` — nunca estimada, nunca
em UTC):

```
🗣️ [2026-07-06 14:02] Arquiteto → Developer [feature/contas--core | sonnet/high]: implementa o endpoint de baixa…
🗣️ [2026-07-06 14:41] Developer → Arquiteto [feature/contas--core | gates verdes]: "entreguei X com N testes…"
🗣️ [2026-07-06 15:07] QA → Arquiteto [feature/contas | HOMOLOGAÇÃO REPROVADA, 2 itens]: "encontrei…"
```

**E entre um handoff e outro, o arquiteto te dá um "ping por etapa"** (também com data e
hora reais)**:** o padrão é reportar o
estado nos pontos naturais — quando a implementação engrena (primeiros commits), quando os
testes e gates do developer ficam verdes, no veredito da homologação e da bateria do QA, e
na conclusão — valendo igual para **developer, QA e mudanças no
próprio fluxo**. Os agentes rodam em segundo plano e não transmitem ao vivo, então o
arquiteto mostra o **estado observável** (branch, worktree ativa, commits já feitos, tempo
decorrido), nunca um progresso inventado. Você escolhe o ritmo por sessão (por etapa /
temporizado curto / primeiro plano / só handoffs) — **por etapa é o padrão**. E o arquiteto,
como orquestrador, **checa de tempos em tempos se algum agente travou** (worktree sem avanço
muito além da estimativa) e, se travou, o caso volta para ele analisar a causa antes de
qualquer outra coisa.

**E cada fatia deixa dois relatórios em `docs/reports/`:** o plano aprovado (com os
critérios de aceite numerados) em `plans/` — só local, **não vai para o git** — e o
relatório de conclusão em `final/` — **versionado, entra no PR** — com a evidência e o
porquê detalhado de cada critério de aceite ter passado, mais a retrospectiva do fluxo
(linha do tempo dos handoffs, reworks e motivos, gargalos e lições aprendidas).

## 6. Receitas rápidas do dia a dia

- **"Quero ver o sistema rodando"** → `/dev-env` — sobe tudo, testa a comunicação e te dá as
  URLs e os logins de teste.
- **"O check do PR ficou vermelho e não entendo por quê"** → `/ci-triage 15` — lê os logs
  certos, diz se é configuração, teste instável, bug real ou snapshot desatualizado, e
  propõe o fix.
- **"Me dá um resumo do que foi feito este mês"** → "relatório executivo de junho" (ao
  arquiteto) — números sempre com a fonte citada, nunca inventados.
- **"Esse PR do fulano tá grande, me ajuda"** → "revisa o PR 17" — briefing com o que merece
  sua atenção antes do merge.
- **"Só quero specs por enquanto"** → "cria specs para X, Y e Z" — o arquiteto especifica com
  você e **para**; nada é implementado sem sua ordem.

## 7. O vai-e-volta (rework)

Como num time real, o fluxo **não é só para frente** — e quem media é o arquiteto:

```
você + arquiteto → spec → plano (com critérios de aceite) → developer (testes ao final)
                                   ▲                              │
                                   │            QA HOMOLOGAÇÃO (valida a spec)
                                   └── rework (mesmo developer) ──┤ reprovou
                                                                  │ complexo? → ARQUITETO
                                              homologação ok ─────▼
                                       QA BATERIA COMPLETA (gates+E2E+PIT)
                                                                  │ QUALQUER erro
                     ARQUITETO analisa a causa raiz e decide:  ◄──┘
       replaneja/divide · revisa · soluciona · reatribui · traz o caso a você

  aprovado → revisão (olhos frescos) → /dod (ACs + retrospectiva) → PR → briefing → VOCÊ mergeia
```

- **Reprovação na HOMOLOGAÇÃO** → os achados voltam para o MESMO developer (ele não recomeça
  do zero — a conversa dele fica preservada). Cada correção exige um teste novo que prove o
  conserto. **Achado complexo demais** (desenho/spec) pula o developer e vai direto ao
  arquiteto.
- **Mais de 1 rework na mesma tarefa** (2ª reprovação), **developer travado ou demorando
  muito além do combinado** → a tarefa **volta para o arquiteto analisar** a causa raiz: gap
  de spec? plano ruim? modelo errado? tarefa grande demais? Ele decide:
  replanejar/dividir, reatribuir (subindo para Opus se for o caso), fazer ele mesmo — ou
  trazer o caso a você.
- **Qualquer erro na BATERIA COMPLETA do QA, CI vermelho no PR ou falha na verificação
  final** → volta **primeiro ao arquiteto** (diagnóstico via `/ci-triage`), nunca direto ao
  developer. Se o CI reprovar de novo depois de um conserto (ciclo de erros), a tarefa
  **fica com o arquiteto** até ele entender a causa raiz.
- **O problema é de desenho** (a spec/plano estava errado) → o arquiteto **replaneja com
  você** — nunca sozinho.

## 8. O que os agentes NUNCA fazem (e por quê)

Proteções de governança (gravadas em `.claude/settings.json` — o assistente é
fisicamente bloqueado, não é só combinado):

| Nunca | Quem faz então |
|---|---|
| Merge de PR (em develop ou main) | **Você**, no GitHub, depois do briefing do arquiteto |
| Criar tag / release | **Você** pede explicitamente; release nasce de PR develop→main |
| Force-push | Ninguém |
| Commitar segredo/senha/chave | Ninguém — o scanner (gitleaks) bloqueia no CI e no pre-commit |

O que eles **podem** (e é o fim normal de toda fatia): fazer commits na branch da fatia,
integrar as sub-branches dos developers na branch da fatia (merge **local** de `feature/*` —
o merge de PR em develop/main continua sendo só seu), dar push da branch e **abrir** o PR
para develop.

## 9. Criar um projeto novo a partir deste

1. Crie o repositório novo (cópia/clone deste template).
2. Abra o Claude Code **no repositório novo** e digite:
   ```
   /new-project meu-produto com.minhaempresa.meuproduto sistema de gestão de clínicas
   ```
3. Ele confirma que NÃO está no template original (guarda de segurança), pergunta sobre o
   domínio, renomeia o pacote, **preserva o método** (regras de arquitetura, gates, este
   toolkit inteiro) e **reseta os artefatos de produto** (specs, manual, changelog, roadmap).
4. Termina com o esqueleto rodando (`docker compose up` → health UP) e a lista do que só
   você pode fazer no GitHub (proteção de branch, secrets, CODEOWNERS).

O detalhe do que é preservado/parametrizado/resetado está em
[`.claude/skills/new-project/parameterization.md`](../.claude/skills/new-project/parameterization.md).

## 10. Perguntas frequentes

**Onde isso tudo fica?** `.claude/skills/` (as receitas) e `.claude/agents/` (o time). São
arquivos de texto Markdown, versionados como código — dá para ler, editar e revisar em PR
como qualquer arquivo. Estão em inglês (instruções para o modelo funcionam melhor assim),
mas **toda a comunicação com você é em português** — é regra escrita em cada um.

**Como edito um comando/agente?** Abra o `.md` correspondente, edite, salve. Vale na hora
(mesma sessão). Mudança relevante entra por PR como tudo mais.

**Preciso decorar os nomes?** Não. Digite `/` e a lista aparece com descrições. Para o resto,
fale com o arquiteto em português — ele sabe o que acionar.

**Quanto custa usar o time inteiro?** Cada agente consome tokens. Por isso o arquiteto tem a
regra de **escala**: fatia pequena = ele mesmo resolve, sem acionar ninguém; o pipeline
completo (developer → QA homologação → QA bateria → revisão → docs) é para fatias que
justificam. Você pode sempre pedir "faz você mesmo, sem delegar".

**E se um developer travar?** Peça o status ao arquiteto. Developers rodam em worktrees
(cópias isoladas) — o trabalho deles sobrevive e pode ser retomado; nada encosta no seu
diretório.

**Isso substitui o TUTORIAL.md?** Não. O [`TUTORIAL.md`](TUTORIAL.md) ensina o **método**
(o laço de 7 passos, como este sistema foi construído). Este guia ensina a **operar o time**
que executa esse método. Leia os dois; este primeiro.
