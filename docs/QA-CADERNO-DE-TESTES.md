# Caderno de Testes QA Humano - FKMed

> Artefato vivo em pt-BR para homologacao manual, testes exploratorios e apoio a QA corporativo.
> Este caderno complementa os testes automatizados; ele nao substitui `backend verify`, frontend
> lint/test/build nem Playwright.

## 1. Objetivo

Guiar uma pessoa de QA por todos os fluxos do portal FKMed, combinando testes funcionais,
negativos, exploratorios, seguranca, LGPD, acessibilidade, performance percebida e operacao.

Use este caderno quando uma fatia for entregue, houver regressao ampla antes de release, uma pessoa
nova precisar entender o produto testando, ou uma mudanca tocar dinheiro, dados pessoais,
autorizacao, documentos clinicos ou reembolso.

## 2. Referencias usadas

Este caderno usa praticas inspiradas em fontes reconhecidas:

- ISTQB CTFL 4.0: fundamentos, desenho de testes, tecnicas caixa-preta, experiencia, risco,
  gestao de defeitos e comunicacao clara.
- OWASP Web Security Testing Guide: checklists de teste de aplicacoes web.
- OWASP API Security Top 10 2023: autorizacao por objeto/funcao, autenticacao, consumo de recursos,
  configuracao e inventario de APIs.
- OWASP File Upload Cheat Sheet: validacao de extensao/tipo/assinatura, limite de tamanho,
  permissao, armazenamento e defesa contra arquivos maliciosos.
- NIST Cybersecurity Framework 2.0: governar, identificar, proteger, detectar, responder e
  recuperar.
- ANPD/LGPD: foco em direitos do titular, minimizacao, seguranca e tratamento responsavel de dados
  pessoais.

Links oficiais:

- https://istqb.org/certifications/certified-tester-foundation-level-ctfl-v4-0/
- https://owasp.org/www-project-web-security-testing-guide/
- https://owasp.org/API-Security/editions/2023/en/0x00-header/
- https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- https://www.nist.gov/cyberframework
- https://www.gov.br/anpd/pt-br/canais_atendimento/cidadao-titular-de-dados

## 3. Como testar com eficiencia

Ordem recomendada:

1. Smoke: app sobe, login funciona, menu carrega, usuario principal acessa Home.
2. Regressao de alto risco: autenticacao, escopo familiar, dinheiro, reembolso, documentos, uploads
   e operator-sim.
3. Jornadas ponta a ponta: uma por area principal.
4. Exploratorio guiado: usar as charters deste documento.
5. Nao funcionais: acessibilidade, responsividade, seguranca, privacidade, performance percebida.
6. Evidencia final: anotar ambiente, build, dados usados, resultado e defeitos.

Tecnicas praticas:

- Particionamento de equivalencia: classes validas/invalidas, como arquivo permitido/proibido.
- Valor limite: exatamente no limite, abaixo e acima, como 2 MB por arquivo ou 20 MB total.
- Tabela de decisao: combinar tipo de reembolso, documentos, data, valor e banco.
- Transicao de estados: guias, token, agendamento, telemedicina e reembolso.
- Teste baseado em risco: priorizar vazamento de dados, dinheiro errado, acesso indevido e perda de
  documento.
- Exploratorio com charter: missao curta, evidencia e aprendizado.

## 4. Ambiente e massa de dados

Ambiente local:

```bash
docker compose up -d
cd frontend && npm start
```

URLs:

- app local: http://localhost:4200
- backend/API/OIDC: http://localhost:8080
- Grafana: http://localhost:3000

Stack isolada de E2E:

```bash
cd frontend
npm run e2e:up
npm run e2e
npm run e2e:down
```

Credenciais dev-only:

| Perfil | Credencial | Uso |
|---|---|---|
| Titular principal | `maria@fkmed.local` / `maria12345` | Jornada completa da titular e dependente |
| Operador simulado | `operador-sim@fkmed.local` / `operador12345` | APIs `/api/sim/**`, somente dev/E2E |
| Plano sem reembolso | `reembolso-sem-direito-e2e@fkmed.local` / `reembolso12345` | Caso negativo de reembolso |

## 5. Inventario de testes automatizados

Backend:

```bash
cd backend && ./mvnw verify
```

| Area | Exemplos de testes |
|---|---|
| Arquitetura e gates | `ArchitectureTest`, `ArchitectureRules`, `ModularityTest`, `HttpErrorMappingCompletenessTest`, `I18nCompletenessTest`, `OpenApiSnapshotIT` |
| Identidade e acesso | `FirstAccessAndVerificationIT`, `LoginAndTokenFlowIT`, `AccountLockoutAndSessionIT`, `ConcurrentFailedLoginIT`, `PasswordPolicyTest`, `UserAccountTest` |
| Plano/contexto | `PlanApiIT`, `ContextApiIT`, `BeneficiaryAccessTest`, `ProtocolGeneratorIT` |
| Auditoria e privacidade | `AuditRetentionIT`, `MaskingTest`, `NotificationContentMaskingTest`, `AuthenticationEventsLoggerTest` |
| Observabilidade/infra | `AccessLogFilterTest`, `UploadTransportConfigurationTest`, `ProdReadinessValidatorTest`, `SystemEndpointsIT` |
| Home/conteudo | `ContentApiIT`, `BannerTest` |
| Perfil/carteirinha | `ProfileApiIT`, `LegalDocumentApiIT`, `CardApiIT`, `CardPdfRendererTest` |
| Rede/agendamento | `NetworkApiIT`, `NetworkSearchTest`, `AppointmentApiIT`, `AppointmentConcurrencyIT`, `AppointmentNotificationIT` |
| Telemedicina | `TeleApiIT`, `TeleSessionLifecycleIT`, `TeleSessionStreamIT`, `TeleConcurrencyIT`, `TeleSchedulingIT` |
| Minha Saude | `ClinicalDocumentApiIT`, `ClinicalDocumentIssuanceIT`, `ClinicalDocumentPdfRendererTest` |
| Guias/token | `GuideApiIT`, `TokenApiIT`, `OperatorSimGuidesIT`, `AttendanceTokenTest` |
| Financeiro | `FinanceApiIT`, `OperatorSimFinanceIT`, `UpdatedAmountTest`, `DigitableLineTest`, `InvoicePdfRendererTest` |
| Atendimento | `SupportApiIT`, `SupportServiceTest` |
| Reembolso | `ReimbursementApiIT`, `ReimbursementPreviewApiIT`, `OperatorSimReimbursementIT`, `ReimbursementDomainTest` |

Frontend/E2E:

```bash
cd frontend
npm run lint
npm test
npm run build
npm run e2e
```

| Arquivo Playwright | Jornada |
|---|---|
| `primeiro-acesso.spec.ts` | Criacao/verificacao de conta |
| `seguranca-conta.spec.ts` | Recuperacao, troca de senha, lockout/sessao |
| `home.spec.ts` | Home, banners, atalhos |
| `meu-plano.spec.ts` | Dados do plano |
| `notificacoes.spec.ts` | Central/preferencias |
| `perfil.spec.ts` | Foto, cadastro, documentos legais |
| `carteirinha.spec.ts` | Carteirinha/PDF |
| `rede.spec.ts` | Rede credenciada |
| `agendamento.spec.ts` | Consulta/exame/cancelar/remarcar |
| `tele.spec.ts` | Pronto Atendimento, sala, teleconsulta |
| `minha-saude.spec.ts` | Documentos clinicos |
| `guias.spec.ts` | Guias e token |
| `financas.spec.ts` | Boletos, validador, copay, IR, quitacao |
| `atendimento.spec.ts` | Canais, FAQ, Libras |
| `reembolso.spec.ts` | Solicitacao, historico, extrato, previas |

## 6. Checklist de smoke

- App abre sem erro bloqueante.
- Login com MARIA funciona.
- Menu lateral aparece.
- Home mostra nome da beneficiaria ativa.
- Seletor de beneficiario alterna titular/dependente.
- Meu Plano carrega.
- Sino de notificacoes abre.
- Logout encerra a sessao.
- Recarregar rota interna mantem/recupera autenticacao esperada.

## 7. Roteiros funcionais por area

### 7.1 Primeiro acesso e verificacao

Feliz:

- Iniciar primeiro acesso.
- Informar CPF, carteirinha e nascimento validos de beneficiario sem conta.
- Criar senha com 8+ caracteres, letra e numero.
- Aceitar termos e privacidade.
- Confirmar orientacao de verificar e-mail.

Negativos/bordas:

- CPF invalido ou com digitos incorretos.
- Carteirinha inexistente.
- Data de nascimento divergente.
- Dependente menor de idade tentando criar conta.
- Senha com 7 caracteres, sem numero, igual ao e-mail.
- Criar conta sem aceitar termos.
- Link de verificacao expirado/invalido.

Exploratorio:

- Trocar maiusculas/minusculas no e-mail.
- Voltar etapas do wizard.
- Confirmar que mensagens nao revelam qual dado esta incorreto.

### 7.2 Login, sessao e seguranca

Feliz:

- Login com MARIA.
- Marcar/desmarcar "Manter conectado".
- Trocar senha autenticado.
- Fazer logout.

Negativos/bordas:

- Senha errada 5 vezes bloqueia temporariamente.
- Durante bloqueio, senha correta nao entra.
- Recuperacao responde de forma neutra para e-mail existente/inexistente.
- Link de reset so pode ser usado uma vez.
- Depois de reset, sessoes antigas caem.

Exploratorio:

- Abrir duas abas e trocar senha em uma.
- Testar voltar do navegador apos logout.
- Verificar mensagens sem enumeracao de conta.

### 7.3 Contexto familiar e autorizacao

- Como titular, alternar MARIA/PEDRO.
- Confirmar que telas refletem o beneficiario ativo.
- Tentar acessar recurso de beneficiario fora da familia por URL/API.
- Como dependente, confirmar que o seletor mostra apenas o proprio beneficiario.
- Trocar beneficiario no meio de fluxos longos e procurar mistura de dados.

### 7.4 Home e Meu Plano

- Home mostra cartao, atalhos, banners e avisos.
- Banner antifraude navega para Atendimento.
- Meu Plano mostra ANS, abrangencia, coparticipacao, reembolso, adicionais e familia.
- Simular erro de rede e confirmar estado de erro + tentar novamente.
- Conferir atalhos indisponiveis e rotas reais.

### 7.5 Notificacoes

- Abrir central pelo sino.
- Marcar uma notificacao como lida.
- Marcar todas como lidas.
- Alterar preferencias de e-mail em evento permitido.
- Evento obrigatorio de seguranca nao permite desativar e-mail.
- Lista vazia orienta o usuario.
- Conteudo nao exibe CPF/CNS/banco completos.

### 7.6 Perfil, documentos legais e foto

- Enviar foto JPG/PNG valida ate 5 MB.
- Ver avatar atualizar sem novo login.
- Remover foto.
- Alterar contato/endereco parcialmente.
- Abrir Termos de Uso e Politica de Privacidade.
- Testar foto acima de 5 MB.
- Testar arquivo com extensao permitida e conteudo invalido.
- Salvar contato sem e-mail/celular obrigatorio.
- Verificar interceptacao quando ha nova versao de termo.

### 7.7 Carteirinha digital

- Abrir carteirinha do beneficiario ativo.
- Copiar numero.
- Baixar PDF.
- Abrir "Minhas Carteirinhas" e selecionar dependente.
- Beneficiario inativo indica indisponibilidade.
- CNS aparece completo apenas na carteirinha/PDF.

### 7.8 Rede credenciada

- Buscar por localidade: UF, municipio, bairro opcional, tipo, especialidade.
- Buscar por nome com 3+ caracteres.
- Abrir detalhe, copiar endereco, acionar rota.
- Busca com menos de 3 caracteres.
- Filtros sem resultado.
- Prestador inativo ou fora da cobertura nao aparece como disponivel.
- Explorar acentos e caixa: "clinica", "clinica" com acento, "CLINICA".

### 7.9 Agendamento

- Agendar consulta: especialidade, unidade, data/hora, revisao, protocolo.
- Agendar exame com pedido medico.
- Ver em Meus Agendamentos.
- Cancelar.
- Reagendar mantendo protocolo.
- Testar horario com menos de 2h de antecedencia.
- Testar data fora dos proximos 30 dias.
- Testar exame sem pedido medico.
- Testar arquivo invalido/maior que 5 MB.
- Testar concorrencia de ultimo slot.

### 7.10 Telemedicina

- Pronto Atendimento: triagem, termo, fila, posicao/ETA, sala, encerramento e documentos.
- Sair da fila.
- Teleconsulta agendada: escolher especialidade/data/hora, confirmar e entrar no periodo permitido.
- Queixa com menos de 10 ou mais de 500 caracteres.
- Sintoma de emergencia alerta.
- Uma sessao ativa por beneficiario.
- Abrir a mesma fila em duas abas.

### 7.11 Minha Saude

- Listar documentos por categoria.
- Filtrar por beneficiario e periodo.
- Abrir detalhe.
- Baixar PDF.
- Em encaminhamento, usar "Agendar consulta".
- Documento expirado continua baixavel com selo correto.
- Dependente fora do escopo nao acessa.
- CID aparece em atestado conforme decisao do produto.

### 7.12 Guias e token

- Listar guias, filtrar por status/periodo.
- Abrir guia autorizada e conferir senha/validade.
- Abrir guia negada e conferir motivo.
- Gerar token, copiar e observar contagem regressiva.
- Gerar novo token invalida anterior.
- Token expirado nao aparece como utilizavel.
- Guia vencida orienta procurar prestador/canais.
- Usar operator-sim para mudar estados e conferir notificacoes.

### 7.13 Financas

- Como titular, abrir boletos em aberto/pagos.
- Abrir boleto vencido e conferir valor atualizado.
- Copiar linha digitavel e PIX.
- Baixar 2a via PDF.
- Validar boleto autentico.
- Abrir coparticipacao, IR e quitacao.
- Dependente nao acessa Financas.
- Linha digitavel com menos/mais de 47 digitos.
- Boleto nao reconhecido orienta nao pagar.
- Ano sem todos os pagamentos nao gera quitacao.

### 7.14 Atendimento, FAQ e Libras

- Abrir canais oficiais.
- WhatsApp abre nova aba.
- Banner antifraude chega na ancora correta.
- FAQ busca por termo e filtra categoria.
- Solicitar atendimento em Libras.
- Busca sem resultado tem empty state.
- Accordion mantem uma pergunta aberta por vez.
- Fora do horario de Libras registra para proximo periodo.

### 7.15 Reembolso

Feliz:

- Plano com direito abre Reembolso.
- Solicitar Consulta: beneficiario, despesa, data, valor, prestador, banco, termo e documento fiscal.
- Conferir protocolo `RE-...`, status atual, prazo e timeline.
- Historico mostra detalhe, documentos, banco mascarado e timeline.
- Enviar documento de pendencia.
- Corrigir dados bancarios apos falha de pagamento.
- Extrato mostra apenas pagos no periodo.
- Previa de Consulta retorna estimativa imediata.
- Previa de Exame/Terapia com anexos fica em analise e pode ser concluida por operator-sim.

Negativos/bordas:

- Plano sem reembolso mostra aviso e nao permite solicitar.
- Despesa fora de 12 meses.
- Valor zero/negativo.
- Sessao de terapia com soma diferente do total.
- Documento obrigatorio faltando.
- Arquivo > 2 MB.
- Total > 20 MB.
- Formato/extensao falsa.
- Conta bancaria de terceiro, PJ ou salario.
- Reenvio com mesma idempotency key nao duplica protocolo.

Exploratorio:

- Montar tabela de decisao: tipo de despesa x documentos obrigatorios x data x valor x banco.
- Testar glosa parcial, negativa, pendencia e pagamento.
- Conferir notificacoes para cada transicao.
- Verificar banco mascarado em todos os lugares.

### 7.16 Operator-sim

- Login/obter token de operador sim em dev/E2E.
- Executar acoes de guias, finance, telemedicina e reembolso.
- Confirmar que a acao move o estado real e gera notificacao/auditoria quando esperado.
- Beneficiario comum nao acessa `/api/sim/**`.
- `app.sim.enabled=false` desabilita rotas.
- Em perfil prod, `ProdReadinessValidator` recusa flag/credencial.

## 8. Seguranca, LGPD e abuso

Autenticacao/sessao:

- Erros de login nao enumeram conta.
- Lockout funciona e expira.
- Token/sessao nao aparecem em URL.
- Logout impede voltar para telas autenticadas.
- Recuperacao de senha nao revela se e-mail existe.

Autorizacao:

- Titular nao acessa familia alheia.
- Dependente nao acessa outro dependente.
- Dependente nao acessa Financas.
- Beneficiario comum nao acessa operator-sim.
- APIs respondem 401/403/404 conforme contrato, sem vazar existencia indevida.

Dados pessoais:

- CPF/CNS/banco mascarados fora dos contextos permitidos.
- Notificacoes nao carregam dados sensiveis completos.
- Logs de autenticacao nao mostram e-mail completo.
- Correlation ID nao aceita texto livre com espacos ou dados pessoais.

Uploads:

- arquivo valido por tipo permitido;
- extensao permitida com conteudo invalido;
- conteudo permitido com nome estranho;
- arquivo acima do limite por arquivo;
- conjunto acima do limite total;
- muitos arquivos pequenos;
- arquivo vazio;
- tentativa de path traversal no nome do arquivo;
- download/acesso ao documento por usuario fora do escopo.

API:

- mudar IDs em URLs/parametros para objeto de outro beneficiario;
- repetir transicoes de estado;
- enviar campos extras inesperados;
- omitir campos obrigatorios;
- testar verbos HTTP errados;
- tentar payload grande em endpoints sensiveis;
- validar se erros nao revelam stack trace.

## 9. Acessibilidade e responsividade

- Navegar por teclado sem mouse.
- Foco visivel em todos os controles.
- Campos com label.
- Botoes icon-only com nome acessivel.
- Dialogs/accordions/tabs operaveis por teclado.
- Contraste legivel.
- Texto sem sobreposicao em 320 px, 768 px e desktop.
- Mensagens de erro associadas aos campos.
- Estados de carregamento e vazio claros.
- Fluxos longos nao perdem contexto ao voltar.

Explorar zoom 200%, nomes muito longos, strings longas de erro, mobile retrato e teclado apenas em
wizards.

## 10. Performance percebida

Checar:

- primeira carga da SPA;
- troca de beneficiario;
- listas grandes;
- upload de arquivos no limite;
- PDFs gerados;
- telas apos erro de rede.

Sinais de defeito:

- tela sem loading;
- clique duplicado criando duplicidade;
- lista travando a UI;
- E2E dependente de timing fragil;
- bundle crescendo sem necessidade.

## 11. Modelo de evidencia

```text
Data/hora:
Ambiente:
Branch/commit:
Build/versao:
Navegador:
Usuario:
Dados usados:
Roteiros executados:
Resultado:
Defeitos abertos:
Riscos residuais:
Evidencias anexadas:
```

Modelo de defeito:

```text
Titulo:
Severidade: Blocker | Critical | High | Medium | Low
Area:
Pre-condicao:
Passos:
Resultado atual:
Resultado esperado:
Frequencia:
Dados usados:
Evidencia:
Hipotese de causa:
Impacto:
```

## 12. Severidade sugerida

| Severidade | Quando usar |
|---|---|
| Blocker | Impede login, app nao sobe, perda/vazamento grave de dados, dinheiro errado sem workaround |
| Critical | Quebra jornada principal de saude/dinheiro/reembolso ou autorizacao critica |
| High | Funcionalidade importante falha, mas ha workaround aceitavel |
| Medium | Inconsistencia funcional limitada, copy enganosa, validacao incompleta |
| Low | Polimento, alinhamento visual, texto menor, melhoria exploratoria |

## 13. Charters exploratorias rapidas

- **Tour do beneficiario titular:** entrar como MARIA e resolver um dia inteiro de uso.
- **Tour do dependente:** tentar todos os caminhos onde o dependente deve ter menos permissao.
- **Tour do dinheiro:** boletos, PIX, linha digitavel, copay, IR, quitacao, reembolso e glosa.
- **Tour de documentos:** foto, pedido medico, documentos clinicos, reembolso e PDFs.
- **Tour de seguranca:** troca de senha, lockout, reset, logout, URLs diretas e operator-sim.
- **Tour de caos leve:** recarregar paginas, voltar navegador, duas abas, duplo clique, rede lenta.
- **Tour de linguagem:** textos longos, acentos, vazio, mensagens de erro e consistencia pt-BR.

## 14. Criterio de saida de uma homologacao

Uma rodada pode ser considerada aprovada quando:

- smoke passou;
- fluxos de alto risco foram executados;
- defeitos Blocker/Critical foram resolvidos ou explicitamente aceitos pelo owner;
- Highs foram avaliados com workaround/risco;
- evidencias foram registradas;
- gates automatizados relevantes estao verdes;
- riscos residuais foram comunicados.
