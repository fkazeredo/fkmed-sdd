# Guias de arquitetura

← Voltar para: [README](../../README.md) · [Índice da documentação](../README.md)

Estes 12 guias são as **regras detalhadas** que valem por área. O [`CLAUDE.md`](../../CLAUDE.md) na
raiz carrega os invariantes de toda tarefa e **roteia** para o guia certo antes de tocar cada área
(veja o *Routing Map* lá). Leia o guia da área **antes** de mexer nela.

| Guia | Leia quando for mexer em… |
|---|---|
| [core-principles.md](core-principles.md) | qualquer decisão de design não trivial (Regra Zero, ordem de autoridade) |
| [backend.md](backend.md) | código backend — serviços, entidades, DTOs, erros, datas, nomes, comentários |
| [frontend-angular.md](frontend-angular.md) | código Angular — componentes, forms, estado, HTTP, UX/teclado |
| [modules-and-apis.md](modules-and-apis.md) | fronteiras de módulo, chamadas entre módulos, REST/GraphQL/gRPC, OpenAPI |
| [persistence.md](persistence.md) | banco, migrações, transações, locking, cache, busca |
| [messaging-and-integrations.md](messaging-and-integrations.md) | eventos, filas, jobs, idempotência, outbox, APIs externas, arquivos, IA/LLM |
| [security.md](security.md) | segurança, autorização, contexto de usuário, LGPD, multi-tenancy |
| [observability.md](observability.md) | logs, métricas, tracing, health, performance |
| [testing.md](testing.md) | escrever ou mudar testes; estratégia de teste |
| [delivery.md](delivery.md) | build, dependências, Git, CI/CD, Docker, deploy, feature flags |
| [simulation-and-mocking.md](simulation-and-mocking.md) | requisito fora de escopo/indeciso; stub de um seam futuro |
| [workflow.md](workflow.md) | specs, ADRs, planos, tarefas grandes; criar projeto novo do template |

> A **stack real com versões** está no topo de [backend.md](backend.md),
> [frontend-angular.md](frontend-angular.md) e [observability.md](observability.md).
