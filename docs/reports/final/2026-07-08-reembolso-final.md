# Relatório de conclusão — Fase 6 "Reembolso"

**Fase 6** · Specs: SPEC-0015, SPEC-0016, SPEC-0017 e ações de reembolso/prévia da SPEC-0018.
Branch `feature/reembolso-solicitacao` → PR #28 para `develop`. Versão **v0.12.0**. Migração **V27**.
Módulo novo **`domain.reimbursement`** (ADR-0022) e kernel **`domain.upload`**.

## 1. Critérios de aceite

| Área | Evidência | Resultado |
|---|---|---|
| Solicitação de reembolso | `ReimbursementApiIT`, `ReimbursementDomainTest`, E2E `reembolso.spec.ts` | Termo, uploads JPG/PNG/PDF, protocolo `RE-...`, idempotência, validações de prazo/contato/banco/documentos e análise automática para `PROCESSAMENTO`. |
| Acompanhamento e análise | `ReimbursementApiIT`, `OperatorSimReimbursementIT`, notificações | Histórico/detalhe/timeline, pendência, resolução, aprovação, negativa, falha de pagamento, correção bancária, pagamento e glosa. |
| Extrato pago | `ReimbursementApiIT`, E2E | Lista apenas `PAGO` no período e soma o total. |
| Prévia | `ReimbursementPreviewApiIT`, `OperatorSimReimbursementIT`, E2E | Consulta conclui na hora; demais tipos aceitam anexos, ficam em análise e podem ser concluídos via operator-sim com disclaimer obrigatório. |
| Plano sem reembolso | E2E `reembolso.spec.ts` + fixture V27/DL-0029 | Exibe o gate informativo "Seu plano não possui reembolso". |
| Operação segura | `ProdReadinessValidatorTest`, `OperatorSimDisabledIT`, auditoria/notificações | `/api/sim/**` segue dev-only/OPERATOR_SIM-only; produção recusa fixture/sim indevidos. |

## 2. Gates finais

- Backend: `./mvnw verify` — **757 testes**, 0 falhas, Checkstyle 0, coverage floors OK.
- PIT focado: `domain.reimbursement` invariants — **108 mutações**, 86% killed, 89% test strength.
- Frontend: `npm run lint`, `npm test`, `npm run build` — lint OK, **524 testes** OK, build OK.
- E2E: `npm run e2e:up`, `npm run e2e`, `npm run e2e:down` — **33/33 Playwright** na stack isolada, stack limpa ao final.

## 3. Correções durante os gates

- V27: fixture de plano sem reembolso trocou card number para não colidir com a V13.
- JPA/schema: documentos de reembolso/prévia usam `bytea` sem `@Lob`, alinhado ao DDL.
- OpenAPI: `ReimbursementStatementView.StatementItem` evita colisão com `ClinicalDocumentListResponse.Item`.
- Notificações: testes de catálogo/preferências atualizados para os 20 eventos após V27.
- Testes de domínio: `ReimbursementDomainTest` cobre CPF/CNPJ, prévia, glosa, pendência, negativa, falha/correção de pagamento e bordas de dias úteis.

## 4. Pendências conhecidas

- Build frontend mantém warning de budget inicial: 916,40 kB contra aviso de 900,00 kB (+16,40 kB). O build passa; tratar como dívida de performance, não blocker da Fase 6.
- PR #28 ainda precisa ser revisado/mergeado pelo owner; tag também permanece decisão do owner.
