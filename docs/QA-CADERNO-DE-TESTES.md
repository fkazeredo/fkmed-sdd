# Caderno Operacional de Testes QA - FKMed

> Manual de homologação em pt-BR para uma pessoa que nunca usou o FKMed.
> Siga os passos na ordem. Não use dados pessoais reais.

## 1. O que será testado

O FKMed é o portal do beneficiário de um plano de saúde. Nele, a pessoa titular pode consultar
seu plano e sua família, acessar a carteirinha, procurar prestadores, agendar atendimentos, usar
telemedicina, consultar documentos clínicos e guias, acompanhar finanças, pedir reembolso e falar
com os canais de atendimento.

Há três papéis importantes:

| Papel | O que pode fazer |
|---|---|
| Titular | Acessa os próprios dados, os dependentes e a área financeira do contrato |
| Dependente | Acessa somente o próprio contexto; não vê finanças nem dados privados do titular |
| Operador simulado | Altera estados de back office em desenvolvimento; não possui uma tela no portal |

Este caderno cobre:

- smoke e regressão funcional de todas as áreas visíveis;
- autorização entre titular e dependente;
- uploads e downloads;
- jornadas que mudam de estado;
- cenários negativos, segurança, privacidade e acessibilidade;
- testes exploratórios e registro de evidências.

Os casos manuais complementam, mas não substituem, os testes automatizados.

## 2. Comece aqui

### 2.1 Pré-requisitos

Peça ao responsável pelo ambiente para confirmar:

- Docker Desktop está aberto e saudável;
- Node.js e npm estão instalados;
- as portas `4201` e `8025` estão livres;
- você está na raiz do repositório FKMed;
- somente uma pessoa executará testes mutáveis nessa stack.

No PowerShell, confirme a raiz:

```powershell
Get-Location
```

O caminho deve terminar em `fkmed-sdd`.

### 2.2 Suba o ambiente descartável

Use sempre a stack E2E neste caderno. Ela tem banco próprio e pode ser apagada sem afetar o
ambiente de desenvolvimento.

```powershell
cd frontend
npm run e2e:up
```

O primeiro build pode levar alguns minutos. Só prossiga quando o comando terminar sem erro.

Abra:

- portal: http://localhost:4201
- caixa de e-mail de teste: http://localhost:8025
- saúde do sistema: http://localhost:4201/api/system/health

Na URL de saúde, o resultado esperado contém:

```json
{"status":"UP","database":"UP"}
```

Se o portal não abrir, execute `docker compose -f ../compose.e2e.yaml ps` dentro de `frontend`.
Não registre defeito funcional enquanto algum serviço estiver `starting`, `unhealthy` ou parado.

### 2.3 Reinicie a massa de dados

Alguns casos criam conta, alteram senha, bloqueiam usuário ou criam registros. Para voltar ao
estado inicial:

```powershell
cd frontend
npm run e2e:down
npm run e2e:up
```

`e2e:down` apaga o banco e os arquivos da stack E2E. Salve evidências antes de executá-lo.

### 2.4 Encerre ao terminar

```powershell
cd frontend
npm run e2e:down
```

O resultado esperado é não restar nenhum contêiner ou volume do projeto `fkmed-e2e`.

## 3. Massa de teste

### 3.1 Usuários

| Identificador | Senha inicial | Uso | Efeito colateral |
|---|---|---|---|
| `maria@fkmed.local` | `maria12345` | Titular principal, família completa | Agendamentos, tokens e solicitações persistem até o reset |
| `pedro@fkmed.local` | `Pedro1234` | Criado no primeiro acesso | Não existe antes do caso `QA-AUT-002` |
| `perfil-e2e@fkmed.local` | `perfilE2e12345` | Alteração de cadastro e logout | Cadastro é modificado |
| `termos-e2e@fkmed.local` | `termosE2e12345` | Aceite legal obrigatório | Aceite é consumido |
| `seguranca-e2e@fkmed.local` | `seguranca12345` | Recuperação, troca e bloqueio | Senha muda e a conta termina bloqueada |
| `reembolso-sem-direito-e2e@fkmed.local` | `reembolso12345` | Plano sem reembolso | Somente leitura |
| `operador-sim@fkmed.local` | `operador12345` | APIs internas `/api/sim/**` | Não navega pela Home |

Dados da família principal:

| Pessoa | Papel | Carteirinha | Outros dados |
|---|---|---|---|
| Maria Clara Souza Lima | Titular | `001234567` | CNS `700000000000001` |
| Pedro Souza Lima | Dependente | `001234575` | CPF `15350946056`; nascimento `20/05/2007` |

O plano esperado é `PLANO MÉDICO - ADESÃO PRATA RJ QP COPART TP`, registro ANS `326305`,
abrangência estadual, com coparticipação e reembolso.

### 3.2 Arquivos artificiais

Não envie documento, receita, foto ou dado bancário real. Crie três arquivos artificiais no
PowerShell:

```powershell
$qa = Join-Path $env:TEMP 'fkmed-qa'
New-Item -ItemType Directory -Force $qa | Out-Null
[IO.File]::WriteAllBytes((Join-Path $qa 'imagem-valida.png'), [Convert]::FromBase64String('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='))
[IO.File]::WriteAllText((Join-Path $qa 'falso.pdf'), 'isto nao e um PDF')
$bytes = New-Object byte[] (2MB + 1)
[IO.File]::WriteAllBytes((Join-Path $qa 'maior-que-2mb.pdf'), $bytes)
$bytes5 = New-Object byte[] (5MB + 1)
[IO.File]::WriteAllBytes((Join-Path $qa 'maior-que-5mb.jpg'), $bytes5)
$doc2mb = New-Object byte[] (2MB)
[byte[]]$pngHeader = 0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a
[Array]::Copy($pngHeader, $doc2mb, $pngHeader.Length)
[IO.File]::WriteAllBytes((Join-Path $qa 'documento-2mb.png'), $doc2mb)
Get-ChildItem $qa
```

Use `imagem-valida.png` para foto, pedido médico e documento de reembolso. Use os outros dois
arquivos grandes e o falso somente nos testes negativos. `documento-2mb.png` é uma carga
sintética para o teste do total de 20 MB, não uma imagem para visualização. O limite por arquivo
de reembolso é 2 MB; o de foto e pedido de exame é 5 MB.

## 4. Como executar e registrar

### 4.1 Estados possíveis

Marque cada caso como:

- `PASSOU`: todos os resultados esperados ocorreram;
- `FALHOU`: um resultado observado divergiu;
- `BLOQUEADO`: ambiente ou massa impediram a execução;
- `N/A`: caso não se aplica ao escopo, com justificativa.

### 4.2 Evidência mínima

Para cada caso, registre:

```text
Caso:
Data/hora:
Build/commit:
Navegador e resolução:
Usuário:
Resultado: PASSOU | FALHOU | BLOQUEADO | N/A
Evidências:
Defeito relacionado:
Observações:
```

Em falha, capture:

- tela inteira, incluindo URL;
- ação exata que falhou;
- esperado e observado;
- horário aproximado;
- usuário e beneficiário ativo;
- protocolo, sem expor token, senha ou conteúdo clínico;
- erros do Console e da aba Network, quando souber coletá-los.

Nunca coloque senha, token OAuth, CPF completo ou documento clínico no relato.

### 4.3 Ordem recomendada

| Pacote | Tempo aproximado | Quando executar |
|---|---:|---|
| A - Smoke | 15 min | Todo novo build |
| B - Regressão principal | 90 a 150 min | Antes de PR/release ou mudança transversal |
| C - Segurança e negativos | 45 a 90 min | Mudanças de identidade, autorização, upload ou dinheiro |
| D - Exploratórios e não funcionais | 45 min ou mais | Homologação de release |

Casos marcados `DESTRUTIVO` devem ser executados por último no pacote ou após reset.

## 5. Pacote A - Smoke

### QA-SMK-001 - Sistema, login e navegação

**Risco:** crítico. **Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra a URL de saúde. | JSON informa aplicação e banco `UP`. |
| 2 | Abra o portal. | Aparece `Entrar no FKMed`, sem tela branca ou erro técnico. |
| 3 | Entre com Maria. | A Home aparece e saúda `MARIA`. |
| 4 | Confira a marca e o título da aba. | Ambos mostram `FKMed`; a página está em pt-BR. |
| 5 | Abra o seletor `Beneficiário ativo` e escolha Pedro. | Saudação e carteirinha mudam para Pedro sem novo login. |
| 6 | Volte para Maria. | O contexto volta para a titular. |
| 7 | Abra `Meu Plano`, `Rede Credenciada`, `Agendamento`, `Telemedicina`, `Minha Saúde`, `Guias e Token`, `Finanças`, `Reembolso` e `Atendimento`. | Cada área abre sem erro bloqueante. |
| 8 | Recarregue uma rota interna com `Ctrl+R`. | A sessão é recuperada e a rota continua utilizável. |

### QA-SMK-002 - Fluxo essencial de negócio

**Risco:** crítico. **Estado:** mutável. **Pré-condição:** `QA-SMK-001` passou.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Em `Rede Credenciada`, busque pelo nome `cardio`. | Há pelo menos um prestador. |
| 2 | Em `Agendamento`, inicie `Agendar Consulta`. | O assistente mostra especialidades. |
| 3 | Escolha a primeira especialidade, unidade, dia e horário livres; avance e confirme. | Surge protocolo iniciado por `AG-`. |
| 4 | Abra `Meus Agendamentos`. | O protocolo aparece em `Próximos`, com estado `Agendado`. |
| 5 | Em `Guias e Token`, gere um token. | Aparecem exatamente seis dígitos e contagem regressiva. |
| 6 | Em `Finanças`, abra o primeiro boleto. | Detalhes e linha digitável de 47 dígitos aparecem. |
| 7 | Em `Atendimento`, pesquise `reembolso` no FAQ. | Somente perguntas relacionadas permanecem na lista. |

## 6. Pacote B - Regressão funcional

### 6.1 Acesso e segurança

#### QA-AUT-001 - Login, erro genérico e logout

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Tente entrar com `maria@fkmed.local` e senha `errada`. | Aparece mensagem genérica de dados inválidos, sem revelar se o e-mail existe. |
| 2 | Entre com a senha correta. | Home carregada. |
| 3 | Abra `Perfil` e selecione `Sair`. | Uma confirmação aparece. |
| 4 | Cancele. | A sessão continua ativa. |
| 5 | Repita e confirme. | Volta ao login. |
| 6 | Tente abrir `/financas` pela barra de endereço. | O sistema exige autenticação. |

#### QA-AUT-002 - Primeiro acesso e verificação por e-mail

**Estado:** DESTRUTIVO. **Pré-condição:** stack recém-resetada. **Pessoa:** Pedro.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `/primeiro-acesso`. | Aparece `Primeiro acesso`. |
| 2 | Informe CPF `15350946056`, carteirinha `001234575` e nascimento `20/05/2007`. | O sistema libera a criação da conta. |
| 3 | Informe e-mail `pedro@fkmed.local` e senha `Pedro1234`. | Os campos são aceitos. |
| 4 | Acione mostrar/ocultar senha. | A visibilidade muda sem alterar o valor. |
| 5 | Marque Termos e Privacidade e envie. | Aparece orientação para verificar o e-mail. |
| 6 | Abra Mailpit em `http://localhost:8025` e selecione a mensagem para Pedro. | A mensagem contém link de verificação apontando para `localhost:4201`. |
| 7 | Abra o link. | Aparece confirmação de e-mail verificado. |
| 8 | Clique para entrar e autentique Pedro. | A Home abre. |
| 9 | Abra `Meu Plano`. | A família tem Maria e Pedro; Pedro é dependente. |

Teste negativo antes do passo 2, se desejado: troque um dígito do CPF ou da carteirinha. O
sistema deve rejeitar a combinação sem informar dados da pessoa encontrada.

#### QA-AUT-003 - Recuperação, troca e bloqueio de senha

**Estado:** DESTRUTIVO. **Pré-condição:** stack recém-resetada. Execute todo o caso em ordem.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `/recuperar-senha`, informe `seguranca-e2e@fkmed.local` e envie. | Mensagem neutra confirma o processamento. |
| 2 | No Mailpit, abra o e-mail mais recente desse destinatário e siga o link. | Tela `Redefinir senha`. |
| 3 | Informe `SegE2eReset01` nos dois campos e confirme. | Redefinição concluída. |
| 4 | Entre com a nova senha. | Login funciona. |
| 5 | Saia e tente a senha inicial `seguranca12345`. | Login é recusado. |
| 6 | Entre novamente com `SegE2eReset01` e abra `Segurança`. | O e-mail correto aparece. |
| 7 | Troque a senha para `SegE2eTroca02`, informando a senha atual. | Confirmação de sucesso. |
| 8 | Saia e entre com `SegE2eTroca02`. | Login funciona. |
| 9 | Saia e faça cinco tentativas com uma senha errada. | Cada tentativa mostra erro genérico. |
| 10 | Na sexta tentativa, use a senha correta. | A conta informa bloqueio temporário. |

Após este caso, resete a stack ou aguarde 15 minutos. Não use essa conta em outro caso.

#### QA-AUT-004 - Aceite obrigatório de nova versão legal

**Estado:** DESTRUTIVO. **Pré-condição:** stack recém-resetada. **Usuário:** `termos-e2e`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Faça login. | O sistema abre o aceite legal antes da Home. |
| 2 | Tente abrir Home ou outra área. | A navegação permanece bloqueada no aceite. |
| 3 | Leia o documento e clique `Li e aceito`. | A Home é liberada. |
| 4 | Saia e entre novamente. | A mesma versão não é solicitada outra vez. |

#### QA-AUT-005 - Aviso de sessão expirada

**Estado:** somente leitura. **Pré-condição:** esteja desautenticado.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `/sessao-expirada`. | Aparece uma explicação clara de que a sessão terminou. |
| 2 | Clique na ação para entrar novamente. | Abre `Entrar no FKMed`. |
| 3 | Faça login como Maria. | Home abre sem ciclo de redirecionamento. |

A expiração automática após uma resposta `401` é coberta por teste de frontend. Em uma sessão
manual curta, não espere o token vencer artificialmente.

### 6.2 Home, plano, notificações e perfil

#### QA-HOM-001 - Home e contexto familiar

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Entre como Maria. | Saudação `Olá, MARIA`, plano `ADESÃO PRATA...` e cartão `001234567`. |
| 2 | Selecione Pedro no topo. | Saudação `Olá, PEDRO` e cartão `001234575`. |
| 3 | Abra o aviso LGPD. | Ele abre e o aviso anteriormente aberto fecha. |
| 4 | Localize o banner `Alerta de golpe!` e abra sua ação. | Abre Atendimento já posicionado na seção antifraude. |

#### QA-PLN-001 - Meu Plano e família

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Meu Plano`. | Nome completo do plano e ANS `326305`. |
| 2 | Confira abrangência, coparticipação e reembolso. | `Estadual`, `Sim` e `Sim`. |
| 3 | Confira o adicional. | `Urg/emerg Nacional Hr - Assistência`. |
| 4 | Confira a família. | Maria titular `001234567`; Pedro dependente `001234575`. |

#### QA-NOT-001 - Central e preferências de notificações

**Estado:** mutável. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Clique no sino. | Abre a central; itens aparecem do mais recente para o mais antigo ou há estado vazio claro. |
| 2 | Se houver item não lido, marque apenas um como lido. | O item e o contador atualizam sem recarregar. |
| 3 | Use `Marcar todas como lidas`, se disponível. | Não restam itens não lidos e a ação desaparece. |
| 4 | Abra `Preferências de notificação`. | Tipos e canais são listados. |
| 5 | Localize um evento obrigatório de conta/segurança. | O controle está bloqueado e não pode ser desligado. |
| 6 | Altere uma preferência opcional, saia da tela e volte. | A escolha permanece salva. |

#### QA-PRF-001 - Cadastro, validação e persistência

**Estado:** mutável. **Usuário:** `perfil-e2e`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Entre e abra `Perfil` > `Alterar cadastro`. | Dados de contato editáveis e dados contratuais protegidos. |
| 2 | Apague o celular. | Erro de obrigatoriedade e botão de salvar desabilitado. |
| 3 | Informe `(21) 98888-7766` e salve. | Mensagem de sucesso. |
| 4 | Recarregue, volte ao cadastro. | O novo celular permanece. |
| 5 | Observe dados contratuais. | Há orientação para corrigi-los nos canais de atendimento; não são editáveis. |

#### QA-PRF-002 - Foto de perfil e upload

**Estado:** mutável. **Usuário:** Maria. **Arquivo:** `imagem-valida.png`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Perfil` > `Alterar foto`. | Beneficiário ativo e foto/placeholder aparecem. |
| 2 | Selecione `imagem-valida.png`. | Surge prévia sem salvar automaticamente. |
| 3 | Salve. | Confirmação; avatar muda no Perfil e no cabeçalho. |
| 4 | Recarregue a página. | A foto continua disponível. |
| 5 | Renomeie `falso.pdf` para `falso.jpg` e tente enviar. | O conteúdo é rejeitado, mesmo com extensão de imagem. |
| 6 | Tente `maior-que-5mb.jpg`. | O tamanho é rejeitado com o limite de 5 MB. |
| 7 | Remova a foto e confirme. | Volta ao placeholder e permanece assim após recarregar. |

#### QA-PRF-003 - Termos e comunicado de privacidade

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Perfil` > `Termos de uso`. | Título, versão vigente, data e corpo do documento. |
| 2 | Volte e abra `Comunicado de privacidade`. | Conteúdo de privacidade, sem reutilizar o texto de Termos. |
| 3 | Recarregue cada URL diretamente. | Documento continua acessível para a sessão autenticada. |
| 4 | Saia e tente abrir as URLs novamente. | O sistema exige login antes de mostrar o conteúdo. |

### 6.3 Carteirinha e rede credenciada

#### QA-CAR-001 - Carteirinha, cópia, PDF e dependente

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Carteirinha`. | Nome de Maria, cartão `001234567`, CNS `700000000000001`, ANS `326305`. |
| 2 | Compare abrangência visual e ficha. | Ambas informam `Estadual`. |
| 3 | Clique `Copiar número`. | Confirmação visual; área de transferência contém `001234567`. |
| 4 | Clique `Salvar Carteirinha`. | Um PDF é baixado e pode ser aberto. |
| 5 | Em `Minhas Carteirinhas`, escolha Pedro. | Nome e cartão mudam para Pedro e `001234575`. |

#### QA-RED-001 - Busca por localidade

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Rede Credenciada` > `Busca de rede`. | Há busca por nome e por localidade. |
| 2 | Sem escolher estado, observe município e bairro. | Permanecem desabilitados. |
| 3 | Escolha `RJ`. | Município é habilitado. |
| 4 | Pesquise e escolha `Rio de Janeiro`. | Bairro é habilitado. |
| 5 | Escolha `Centro` e clique `Buscar`. | A próxima etapa resume `Centro, Rio de Janeiro - RJ`. |
| 6 | Escolha `Consultórios/Clínicas/Terapias`. | O sistema pede especialidade. |
| 7 | Pesquise `cardio` e escolha `Cardiologia`. | Lista com pelo menos dez prestadores e data de referência. |
| 8 | Clique `Pesquisar por localidade`. | RJ, Rio de Janeiro e Centro continuam preenchidos. |

#### QA-RED-002 - Busca por nome e detalhe

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Na busca por nome, digite menos de três caracteres. | A busca não é executada ou mostra orientação de mínimo. |
| 2 | Digite `cardio` e busque. | Há ao menos um resultado com tipo de serviço. |
| 3 | Abra o primeiro prestador. | Detalhe mostra especialidades, endereço e telefone. |
| 4 | Clique `Copiar endereço`. | Confirmação visual e endereço na área de transferência. |
| 5 | Clique `Traçar rota`. | Nova aba abre em `google.com/maps` com o endereço. |

### 6.4 Agendamento

#### QA-AGE-001 - Agendar consulta

**Estado:** mutável. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Agendamento` > `Agendar Consulta`. | Etapa 1 de 4, especialidade. |
| 2 | Tente avançar sem escolher. | Mensagem pede uma especialidade. |
| 3 | Escolha a primeira especialidade e avance. | Etapa de unidade. |
| 4 | Escolha a primeira unidade e avance. | Etapa de data e horário. |
| 5 | Escolha o primeiro dia e um horário habilitado. | Seleção fica visível. |
| 6 | Avance. | Revisão mostra beneficiário, especialidade, unidade, data e hora. |
| 7 | Confirme. | Mensagem de sucesso e protocolo iniciado por `AG-`. |
| 8 | Abra `Meus Agendamentos`. | Protocolo em `Próximos`, estado `Agendado`. |

#### QA-AGE-002 - Pedido médico e agendamento de exame

**Estado:** mutável. **Usuário:** Maria. **Arquivo:** `imagem-valida.png`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Agendar Exame` e escolha o primeiro exame. | Etapa 2 de 5, pedido médico. |
| 2 | Avance sem anexar. | O sistema bloqueia e pede o arquivo. |
| 3 | Selecione `falso.pdf`. | Arquivo é rejeitado por conteúdo inválido. |
| 4 | Selecione `imagem-valida.png`. | Nome do arquivo aparece e pode ser removido. |
| 5 | Avance, escolha unidade, dia e horário livres. | Revisão inclui o nome do pedido. |
| 6 | Confirme. | Protocolo `AG-` e exame em `Próximos`. |

#### QA-AGE-003 - Cancelar e reagendar

**Estado:** mutável. **Pré-condição:** existe agendamento futuro criado neste ciclo.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Em `Meus Agendamentos`, localize o protocolo. | Card em `Próximos`. |
| 2 | Clique `Cancelar`, informe `Imprevisto` e confirme. | Card deixa `Próximos`. |
| 3 | Abra `Histórico`. | Mesmo protocolo aparece como `Cancelado`. |
| 4 | Crie outra consulta. | Novo protocolo e horário disponível. |
| 5 | Use `Reagendar` em um agendamento futuro, escolha outro horário e confirme. | Protocolo é mantido e estado/horário são atualizados. |

### 6.5 Telemedicina

#### QA-TEL-001 - Triagem, termo, fila e saída

**Estado:** mutável. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Telemedicina` > `Pronto Atendimento`. | Triagem para o beneficiário ativo. |
| 2 | Informe uma queixa com menos de 10 caracteres e tente avançar. | O sistema pede 10 a 500 caracteres. |
| 3 | Informe `Dor de cabeça há dois dias, sem melhora.`, marque um sintoma e uma duração. | Avanço liberado. |
| 4 | No termo, tente entrar sem aceitar. | Entrada bloqueada. |
| 5 | Aceite e entre na fila. | Posição e estimativa aparecem e atualizam automaticamente. |
| 6 | Clique `Sair da fila`. | Confirmação alerta que a posição será liberada. |
| 7 | Confirme. | Volta ao hub e não há sessão ativa. |

Para sintomas marcados como emergência, deve aparecer orientação de pronto-socorro. O sistema só
pode continuar após confirmação explícita do risco.

#### QA-TEL-002 - Atendimento concluído pelo simulador

**Estado:** mutável. **Restrição:** não existe tela de operador.

Esta jornada tem dois atores. Para não exigir que o QA manipule token OAuth e API manualmente,
resete a stack e execute o roteiro Playwright dedicado em modo visível:

```powershell
cd frontend
npx playwright test e2e/tele.spec.ts --headed --workers=1
```

Resultado esperado:

1. Maria entra na fila pela interface.
2. O operador simulado inicia o atendimento como `Dra. Ana Prado`, `CRM-RJ 54321`.
3. A sala abre automaticamente para Maria.
4. O operador encerra com orientação e receita de `Paracetamol 750mg`.
5. O resumo exibe o documento.
6. `Ver em Minha Saúde` abre a lista com o receituário.
7. O segundo cenário confirma que sair da fila abandona a sessão.

Registre o caso como bloqueado, e não como falha, se a política da homologação proibir automação
assistida. Não procure uma interface de operador: ela não faz parte do produto atual.

#### QA-TEL-003 - Agendar teleconsulta

**Estado:** mutável. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Telemedicina` > `Agendar Consulta`. | Etapa de especialidade. |
| 2 | Escolha especialidade, dia e horário livres. | Revisão informa modalidade Telemedicina. |
| 3 | Confirme. | Protocolo e sucesso. |
| 4 | Abra `Meus Agendamentos` da Telemedicina. | Teleconsulta em `Próximas`. |
| 5 | Tente entrar fora da janela de 10 minutos. | Ação indisponível ou mensagem de janela fechada. |

### 6.6 Minha Saúde, guias e token

#### QA-SAU-001 - Documentos clínicos e PDF

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Minha Saúde` > `Receituários/Atestados`. | Lista do mais recente para o mais antigo. |
| 2 | Abra um receituário com validade. | Profissional, emissão, validade e medicamentos. |
| 3 | Baixe o PDF. | Arquivo `.pdf` abre e corresponde ao documento selecionado. |
| 4 | Volte e abra um atestado, sem selo de validade. | Exibe período de afastamento e CID; não inventa validade. |
| 5 | Abra `Solicitação de Exames` e altere o período. | Lista é recarregada sem erro. |

#### QA-SAU-002 - Encaminhamento para agendamento

**Estado:** somente leitura até iniciar o assistente. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Minha Saúde` > `Encaminhamentos`. | Há encaminhamentos da massa. |
| 2 | Abra o primeiro. | Especialidade-alvo e motivo aparecem. |
| 3 | Clique `Agendar consulta`. | Abre o assistente de consulta. |
| 4 | Observe a especialidade. | Já está selecionada conforme o encaminhamento. |

#### QA-GUI-001 - Guias, filtros e isolamento familiar

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Guias e Token`. | Guias de Maria e filtros de status/período. |
| 2 | Abra uma guia. | Tipo, prestador, data, itens TUSS e status. |
| 3 | Aplique um filtro de status. | Apenas estados compatíveis permanecem. |
| 4 | Selecione Pedro. | Estado vazio orienta que novas guias aparecerão ali. |
| 5 | Clique para atualizar. | A tela permanece consistente e sem erro. |

#### QA-GUI-002 - Autorização de guia pelo simulador

**Estado:** mutável. **Restrição:** não existe tela de operador.

Resete a stack antes deste caso. Em seguida:

```powershell
cd frontend
npx playwright test e2e/guias.spec.ts --headed --workers=1
```

Resultado esperado no primeiro cenário:

- o simulador cria uma guia `Em análise` para Maria;
- autoriza com senha `AUT-777001` e validade `31/12/2026`;
- após `Atualizar`, a guia aparece `Autorizada`;
- o detalhe mostra senha e validade.

Os demais cenários do arquivo validam token e estado vazio de Pedro.

#### QA-TOK-001 - Gerar, copiar e renovar token

**Estado:** mutável. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Em `Guias e Token`, clique `Gerar token` ou `Gerar novo token`. | Código de exatamente seis dígitos. |
| 2 | Observe a contagem. | Inicia próxima de 10 minutos e diminui. |
| 3 | Clique `Copiar`. | Confirmação e mesmo código na área de transferência. |
| 4 | Anote o código e clique `Gerar novo token`. | Novo código diferente; o anterior deixa de ser o ativo. |

### 6.7 Finanças

#### QA-FIN-001 - Boletos, cópia e PDF

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Finanças`. | Aba `Em aberto` com boletos abertos e vencidos. |
| 2 | No vencido, confira o cálculo. | Valor original, multa de 2%, juros, dias e total atualizado. |
| 3 | Abra o primeiro boleto. | Linha de exatamente 47 dígitos, valor e vencimento. |
| 4 | Copie a linha. | Área de transferência contém exatamente os 47 dígitos exibidos. |
| 5 | Copie o PIX. | Confirmação e código começando pelo padrão PIX `000201...`. |
| 6 | Baixe a 2ª via. | PDF baixado e legível. |
| 7 | Volte e abra `Pagos`. | Há data de pagamento nos itens. |

#### QA-FIN-002 - Validador antifraude

**Estado:** somente leitura. **Pré-condição:** copie uma linha real em `QA-FIN-001`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Validar boleto`, cole a linha real com espaços. | Resultado `Boleto autêntico`. |
| 2 | Informe menos de 47 dígitos. | Erro de formato; nenhuma autenticidade afirmada. |
| 3 | Informe 47 dígitos `1`. | `Boleto não reconhecido` e alerta `Não realize o pagamento`. |

#### QA-FIN-003 - Coparticipação e declarações

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Coparticipação`, selecione `Últimos 3 meses`. | Utilizações da família e total do período. |
| 2 | Filtre por beneficiário. | Lista e total refletem somente a pessoa escolhida. |
| 3 | Abra `Imposto de Renda`. | Há ano-base com botão de PDF. |
| 4 | Baixe o demonstrativo. | PDF legível e correspondente ao ano. |
| 5 | Abra `Quitação`. | Apenas ano totalmente quitado está disponível. |
| 6 | Baixe a declaração. | PDF legível e correspondente ao ano. |

#### QA-FIN-004 - Finanças exclusivas do titular

**Estado:** somente leitura. **Usuário:** Maria com Pedro ativo.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Selecione Pedro no topo. | A opção `Finanças` desaparece do menu. |
| 2 | Digite `/financas` diretamente na URL. | Tela `Área exclusiva do titular`; nenhum boleto ou valor de Maria é exibido. |
| 3 | Volte para Maria. | A área financeira volta a ficar acessível. |

### 6.8 Reembolso

#### QA-REI-001 - Nova solicitação completa

**Estado:** mutável. **Usuário:** Maria. **Arquivo:** `imagem-valida.png`.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Reembolso` > `Solicitar`. | Formulário, termo vigente e Maria selecionada. |
| 2 | Selecione `Consulta`; use a data de hoje e valor `150,00`. | Dados aceitos. |
| 3 | Informe prestador `Clínica QA`, especialidade `Cardiologia`, CRM `123456`/`RJ` e CPF/CNPJ `11222333000181`. | Campos preenchidos. |
| 4 | Informe banco `001`, agência `1234`, conta `56789`, dígito `0`. | Dados bancários preenchidos. |
| 5 | Anexe `imagem-valida.png` como `Recibo/nota fiscal`. | Arquivo listado na categoria correta. |
| 6 | Envie sem aceitar o termo. | Solicitação bloqueada com pedido de aceite. |
| 7 | Aceite o termo e envie. | Protocolo iniciado por `RE-`, estado inicial e previsão. |
| 8 | Abra `Histórico` e localize o protocolo. | Mesmos beneficiário, tipo e valor. |

Se o documento obrigatório variar por tipo de despesa, siga a mensagem exibida e anexe somente
arquivos artificiais nas categorias solicitadas.

#### QA-REI-002 - Validações de upload

**Estado:** não conclua o envio. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Anexe `falso.pdf`. | Rejeição: somente JPG, PNG ou PDF válido. |
| 2 | Anexe `maior-que-2mb.pdf`. | Rejeição: arquivo excede 2 MB. |
| 3 | Anexe e remova `imagem-valida.png`. | Item entra e sai da lista sem envio residual. |
| 4 | Tente uma data futura ou valor zero. | Solicitação rejeitada com mensagem de dado inválido. |
| 5 | Selecione Terapia/Psicologia e crie sessões cuja soma difere do total. | Erro informa que a soma deve ser igual ao valor total. |

Para o limite acumulado de 20 MB, use dez arquivos válidos de 2 MB e tente adicionar conteúdo
adicional. Você pode selecionar `documento-2mb.png` repetidamente, pois o campo é limpo após cada
seleção. O conjunto deve ser rejeitado sem travar a página.

#### QA-REI-003 - Histórico, extrato e glosa

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Histórico`. | Existe protocolo seed `RE-20260601-0001`. |
| 2 | Abra o protocolo. | Estado `Pago`, solicitado `R$ 150,00`, reembolsado `R$ 120,00`. |
| 3 | Confira a glosa. | `R$ 30,00` e motivo `Valor excede a tabela do plano`. |
| 4 | Confira a linha do tempo e a conta. | Eventos em ordem e dados bancários mascarados/limitados ao necessário. |
| 5 | Abra `Extrato`, filtre junho de 2026. | Protocolo aparece e total é `R$ 120,00`. |

#### QA-REI-004 - Prévia e plano sem direito

**Estado:** mutável na prévia.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Como Maria, abra `Prévias`, selecione Consulta e solicite. | Protocolo `PV-`, estimativa `R$ 120,00` e aviso de que não é garantia. |
| 2 | Selecione um tipo diferente de Consulta e tente sem orçamento/pedido. | O sistema exige os anexos. |
| 3 | Saia e entre como `reembolso-sem-direito-e2e@fkmed.local`. | Home abre normalmente. |
| 4 | Abra `/reembolso`. | Mensagem informa que o plano não possui reembolso; formulário e histórico não vazam. |

#### QA-REI-005 - Pendência, aprovação, pagamento e correção bancária

**Estado:** mutável. **Restrição:** não existe tela de operador para iniciar as transições.

O fluxo de beneficiário mostra pendência e correção bancária, mas a criação desses estados vem do
back office. A suíte de integração executa os dois atores com dados isolados:

```powershell
cd backend
.\mvnw.cmd "-Dtest=OperatorSimReimbursementIT" test
```

Resultado esperado:

1. solicitação aprovada e paga chega a `Pago`;
2. repetir o pagamento não cria uma segunda transição ou notificação;
3. solicitação negada não pode ser paga;
4. falha de pagamento chega a `Pagamento não efetuado`;
5. a correção bancária do beneficiário retoma o pagamento;
6. pendência mostra a descrição, aceita novo documento e volta a `Processamento`;
7. Maria recebe `403` ao tentar chamar uma rota reservada ao operador.

Na homologação manual integrada com um back office real, repita os passos 4 a 6 pela tela:

- abra o protocolo no Histórico;
- confirme o banner e a descrição da pendência/falha;
- anexe `imagem-valida.png` em uma pendência e envie;
- corrija banco/agência/conta em falha de pagamento;
- atualize a tela e confirme o novo estado e a linha do tempo.

### 6.9 Atendimento

#### QA-ATE-001 - Canais oficiais e antifraude

**Estado:** somente leitura. **Usuário:** Maria.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Na Home, abra o banner `Alerta de golpe!`. | Atendimento abre na seção antifraude. |
| 2 | Confira as orientações. | Não compartilhar senha/token, validar boleto e usar canais oficiais. |
| 3 | Confira `Central 24h`. | Dois telefones com links `tel:`. |
| 4 | Confira WhatsApp sem enviar mensagem. | Link HTTPS para `wa.me` abre em nova aba. |
| 5 | Use o link do validador. | Abre o validador antifraude de boleto. |

#### QA-ATE-002 - FAQ e Central de Libras

**Estado:** mutável apenas na solicitação de Libras.

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Abra `Perguntas frequentes`. | Pelo menos 12 perguntas e categorias. |
| 2 | Pesquise `reembolso`. | Todas as perguntas visíveis contêm o termo/contexto. |
| 3 | Limpe a busca. | Lista completa volta. |
| 4 | Abra a primeira e depois a segunda pergunta. | A segunda abre e a primeira fecha. |
| 5 | Pesquise `termo-sem-nenhum-resultado-possivel`. | Estado vazio repete o termo buscado. |
| 6 | Abra `Central de Libras` e clique `Solicitar atendimento em Libras`. | Confirma atendimento em instantes ou no próximo período, conforme horário. |

### 6.10 Catálogo de casos atômicos

As jornadas anteriores ensinam o caminho completo. Os casos abaixo desdobram a regressão para que
cada comportamento tenha resultado próprio.

Como executar uma variação:

1. Abra a jornada indicada na coluna `Base`.
2. Siga os passos até a tela ou etapa citada.
3. Substitua somente a ação indicada em `Variação`.
4. Compare com `Esperado` e encerre o caso.
5. Volte ao estado inicial ou faça o reset indicado antes do próximo caso.

Na coluna `Base`, `AUT-002` significa a jornada `QA-AUT-002`, e assim por diante.

Prioridade: `P0` bloqueia release; `P1` cobre regra importante; `P2` amplia qualidade. `Especial`
significa que a massa precisa ser preparada pelo responsável do ambiente. Sem ela, marque
`BLOQUEADO`, não `FALHOU`.

#### 6.10.1 Identidade, autenticação e sessão - 24 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| AUT-V001 | P0 | AUT-001 | Login de Maria com senha correta | Home e sessão autenticada | Leitura |
| AUT-V002 | P0 | AUT-001 | Senha incorreta | Erro genérico; não informa se e-mail existe | Leitura |
| AUT-V003 | P1 | AUT-001 | E-mail inexistente | Mesma mensagem do caso de senha incorreta | Leitura |
| AUT-V004 | P1 | AUT-001 | E-mail com letras maiúsculas/espaços externos | Normalização prevista ou erro de validação claro; nunca erro técnico | Leitura |
| AUT-V005 | P0 | AUT-002 | CPF com um dígito alterado | Identidade não validada; nenhum dado revelado | Reset |
| AUT-V006 | P0 | AUT-002 | Carteirinha com um dígito alterado | Identidade não validada | Reset |
| AUT-V007 | P0 | AUT-002 | Nascimento divergente | Identidade não validada | Reset |
| AUT-V008 | P1 | AUT-002 | CPF formatado `153.509.460-56` | Aceito ou rejeitado com formato claro, sem 500 | Reset |
| AUT-V009 | P1 | AUT-002 | Avançar com campo obrigatório vazio | Campo identificado e etapa não avança | Reset |
| AUT-V010 | P0 | AUT-002 | Criar conta sem Termos | Envio bloqueado | Reset |
| AUT-V011 | P0 | AUT-002 | Criar conta sem Privacidade | Envio bloqueado | Reset |
| AUT-V012 | P1 | AUT-002 | Senha com 7 caracteres | Política de senha exibida; conta não criada | Reset |
| AUT-V013 | P1 | AUT-002 | Senha sem número | Política de senha exibida; conta não criada | Reset |
| AUT-V014 | P1 | AUT-002 | Senha sem letra | Política de senha exibida; conta não criada | Reset |
| AUT-V015 | P0 | AUT-002 | Tentar login antes de verificar o e-mail | Acesso recusado sem ativar a conta | Reset |
| AUT-V016 | P0 | AUT-002 | Abrir link de verificação alterando um caractere do token | Link inválido; conta não ativada | Reset |
| AUT-V017 | P1 | AUT-002 | Abrir link de verificação já utilizado | Não duplica ativação; informa link inválido/usado | Reset |
| AUT-V018 | P1 | AUT-003 | Recuperar e-mail inexistente | Mensagem neutra igual à de e-mail existente | Reset |
| AUT-V019 | P1 | AUT-003 | Nova senha e confirmação diferentes | Redefinição bloqueada | Reset |
| AUT-V020 | P0 | AUT-003 | Reutilizar link de redefinição consumido | Link recusado; senha não muda | Reset |
| AUT-V021 | P1 | AUT-003 | Troca autenticada com senha atual errada | Troca recusada; senha vigente continua funcionando | Reset |
| AUT-V022 | P0 | AUT-003 | Quinta falha consecutiva | Próxima tentativa informa bloqueio temporário | Destrutivo |
| AUT-V023 | P0 | AUT-001 | Confirmar logout e usar Voltar | Conteúdo privado não fica utilizável | Leitura |
| AUT-V024 | P1 | AUT-005 | Ação da tela de sessão expirada | Login abre sem loop de redirecionamento | Leitura |

#### 6.10.2 Home, plano e notificações - 18 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| HOM-V001 | P0 | HOM-001 | Login de Maria | Saudação, plano e carteirinha de Maria | Leitura |
| HOM-V002 | P0 | HOM-001 | Trocar para Pedro | Todos os dados visíveis mudam juntos | Leitura |
| HOM-V003 | P0 | HOM-001 | Trocar rapidamente Maria/Pedro | Resultado final corresponde à última seleção | Leitura |
| HOM-V004 | P1 | HOM-001 | Abrir dois avisos em sequência | Somente um permanece aberto | Leitura |
| HOM-V005 | P1 | HOM-001 | Acionar banner antifraude | Atendimento abre na âncora correta | Leitura |
| HOM-V006 | P2 | HOM-001 | Avançar/voltar carrossel de banners | Banner não duplica visualmente nem perde ação | Leitura |
| PLN-V001 | P0 | PLN-001 | Conferir ANS | Exatamente `326305` | Leitura |
| PLN-V002 | P0 | PLN-001 | Conferir membros | Maria titular e Pedro dependente, sem terceiros | Leitura |
| PLN-V003 | P1 | PLN-001 | Recarregar a rota | Dados permanecem consistentes | Leitura |
| PLN-V004 | P0 | SEC-001 | Pedro tenta detalhe privado de Maria | Não encontrado, sem confirmação de existência | Leitura |
| NOT-V001 | P1 | NOT-001 | Abrir sino com itens | Ordem do mais recente para o mais antigo | Mutável |
| NOT-V002 | P1 | NOT-001 | Marcar um item | Contador diminui imediatamente | Mutável |
| NOT-V003 | P1 | NOT-001 | Marcar todos | Contador zera e ação desaparece | Mutável |
| NOT-V004 | P2 | NOT-001 | Reabrir central após marcar | Estado lido persiste | Mutável |
| NOT-V005 | P0 | NOT-001 | Tentar desligar evento obrigatório | Controle bloqueado | Leitura |
| NOT-V006 | P1 | NOT-001 | Alternar preferência opcional | Salva e persiste ao reabrir | Mutável |
| NOT-V007 | P1 | NOT-001 | Central sem itens, com massa especial | Estado vazio orientativo, sem erro | Especial |
| NOT-V008 | P0 | NOT-001 | Abrir link de notificação de outro beneficiário, massa especial | Escopo familiar é respeitado | Especial |

#### 6.10.3 Perfil, legal e carteirinha - 22 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| PRF-V001 | P0 | PRF-001 | Salvar celular válido | Sucesso e persistência | Mutável |
| PRF-V002 | P1 | PRF-001 | Celular vazio | Erro obrigatório; salvar desabilitado | Mutável |
| PRF-V003 | P1 | PRF-001 | Celular incompleto | Formato recusado | Mutável |
| PRF-V004 | P1 | PRF-001 | CEP inválido | Campo identificado; nada salvo | Mutável |
| PRF-V005 | P1 | PRF-001 | UF inválida | Campo identificado; nada salvo | Mutável |
| PRF-V006 | P0 | PRF-001 | Tentar editar dado contratual | Campo protegido e orientação de atendimento | Leitura |
| PRF-V007 | P0 | PRF-002 | Enviar PNG válido | Prévia, salvamento e persistência | Mutável |
| PRF-V008 | P1 | PRF-002 | Selecionar e cancelar antes de salvar | Foto anterior permanece | Mutável |
| PRF-V009 | P1 | PRF-002 | Arquivo falso com extensão JPG | Rejeitado pelo conteúdo | Mutável |
| PRF-V010 | P1 | PRF-002 | Arquivo com 5 MB + 1 byte | Rejeitado pelo tamanho | Mutável |
| PRF-V011 | P1 | PRF-002 | Remover foto | Placeholder persiste após reload | Mutável |
| PRF-V012 | P0 | PRF-002 | Trocar beneficiário antes do upload | Foto é associada somente ao beneficiário exibido | Mutável |
| LEG-V001 | P0 | AUT-004 | Login com versão pendente | Navegação interceptada | Destrutivo |
| LEG-V002 | P0 | AUT-004 | Tentar outra rota sem aceitar | Permanece no aceite | Destrutivo |
| LEG-V003 | P1 | AUT-004 | Aceitar versão atual | Home liberada e aceite não repete | Destrutivo |
| LEG-V004 | P1 | PRF-003 | Abrir Termos | Título, versão, publicação e corpo corretos | Leitura |
| LEG-V005 | P1 | PRF-003 | Abrir Privacidade | Documento correto e distinto de Termos | Leitura |
| CAR-V001 | P0 | CAR-001 | Carteirinha de Maria | Nome, cartão, CNS, ANS e abrangência corretos | Leitura |
| CAR-V002 | P0 | CAR-001 | Selecionar Pedro | Nome e número mudam para Pedro | Leitura |
| CAR-V003 | P1 | CAR-001 | Copiar número de Maria | Nove dígitos exatos no clipboard | Leitura |
| CAR-V004 | P1 | CAR-001 | Baixar PDF de Maria e Pedro | Cada PDF corresponde à pessoa selecionada | Leitura |
| CAR-V005 | P0 | CAR-001 | Usar URL de PDF de Maria em sessão de Pedro | Download negado sem conteúdo de Maria | Especial |

#### 6.10.4 Rede credenciada - 18 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| RED-V001 | P0 | RED-001 | Abrir busca por localidade | Município/bairro inicialmente bloqueados | Leitura |
| RED-V002 | P1 | RED-001 | Selecionar RJ | Município habilitado | Leitura |
| RED-V003 | P1 | RED-001 | Filtrar `rio de` em minúsculas | Rio de Janeiro encontrado | Leitura |
| RED-V004 | P1 | RED-001 | Filtrar nome sem acento | Busca continua insensível a acento/caso | Leitura |
| RED-V005 | P1 | RED-001 | Trocar estado após escolher município | Município e bairro dependentes são limpos | Leitura |
| RED-V006 | P1 | RED-001 | Escolher bairro `Todos` | Busca considera todo o município | Leitura |
| RED-V007 | P0 | RED-001 | Centro + Consultórios + Cardiologia | Dez ou mais resultados esperados na seed | Leitura |
| RED-V008 | P1 | RED-001 | Conferir resumo da localidade | `Centro, Rio de Janeiro - RJ` | Leitura |
| RED-V009 | P1 | RED-001 | Voltar para pesquisar localidade | Seleções anteriores preservadas | Leitura |
| RED-V010 | P1 | RED-002 | Nome com 2 caracteres | Busca bloqueada/orientada | Leitura |
| RED-V011 | P0 | RED-002 | Nome `cardio` | Ao menos um prestador com tipo de serviço | Leitura |
| RED-V012 | P1 | RED-002 | Nome inexistente | Estado vazio e ação para alterar busca | Leitura |
| RED-V013 | P1 | RED-002 | Abrir detalhe | Endereço, telefone e especialidades | Leitura |
| RED-V014 | P1 | RED-002 | Copiar endereço | Conteúdo exato e confirmação visual | Leitura |
| RED-V015 | P1 | RED-002 | Traçar rota | Nova aba segura do Google Maps | Leitura |
| RED-V016 | P2 | RED-002 | Voltar do detalhe | Resultado e critérios preservados | Leitura |
| RED-V017 | P1 | RED-001 | Falha de rede durante busca | Erro controlado e tentativa novamente | Leitura |
| RED-V018 | P2 | RED-001 | Resultado em 320 px | Cartões legíveis, sem rolagem horizontal global | Leitura |

#### 6.10.5 Agendamento - 24 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| AGE-V001 | P0 | AGE-001 | Avançar sem especialidade | Etapa bloqueada com mensagem | Leitura |
| AGE-V002 | P1 | AGE-001 | Escolher especialidade | Somente unidades compatíveis | Leitura |
| AGE-V003 | P0 | AGE-001 | Avançar sem unidade | Etapa bloqueada | Leitura |
| AGE-V004 | P1 | AGE-001 | Unidade sem agenda, massa especial | Estado vazio orientativo | Especial |
| AGE-V005 | P0 | AGE-001 | Avançar sem horário | Etapa bloqueada | Leitura |
| AGE-V006 | P1 | AGE-001 | Horário indisponível | Controle desabilitado | Leitura |
| AGE-V007 | P0 | AGE-001 | Confirmar consulta válida | Protocolo único `AG-` | Mutável |
| AGE-V008 | P0 | AGE-001 | Conferir `Próximos` | Mesmo protocolo e estado Agendado | Mutável |
| AGE-V009 | P1 | AGE-001 | Duplo clique em confirmar | Não cria dois agendamentos | Mutável |
| AGE-V010 | P0 | AGE-001 | Duas abas tentam o mesmo horário | Uma confirma; outra recebe conflito controlado | Mutável |
| AGE-V011 | P0 | AGE-002 | Exame sem pedido | Avanço bloqueado | Leitura |
| AGE-V012 | P1 | AGE-002 | Pedido JPG/PNG/PDF válido | Arquivo aceito e nome exibido | Mutável |
| AGE-V013 | P1 | AGE-002 | Arquivo falso renomeado PDF | Conteúdo rejeitado | Leitura |
| AGE-V014 | P1 | AGE-002 | Pedido acima de 5 MB | Tamanho rejeitado | Leitura |
| AGE-V015 | P1 | AGE-002 | Remover pedido | Volta a bloquear o avanço | Leitura |
| AGE-V016 | P0 | AGE-002 | Confirmar exame válido | Protocolo e item em Próximos | Mutável |
| AGE-V017 | P0 | AGE-003 | Cancelar e confirmar | Sai de Próximos e entra no Histórico | Mutável |
| AGE-V018 | P1 | AGE-003 | Abrir cancelar e voltar | Agendamento permanece | Mutável |
| AGE-V019 | P1 | AGE-003 | Cancelar com motivo vazio | Segue conforme campo opcional | Mutável |
| AGE-V020 | P0 | AGE-003 | Reagendar para horário livre | Protocolo mantido e horário alterado | Mutável |
| AGE-V021 | P0 | AGE-003 | Reagendar para horário ocupado | Conflito controlado; compromisso original preservado | Especial |
| AGE-V022 | P1 | AGE-003 | Filtrar por Pedro | Somente compromissos do dependente | Mutável |
| AGE-V023 | P0 | SEC-001 | Pedro abre agendamento de Maria por ID | Não encontrado | Especial |
| AGE-V024 | P1 | AGE-001 | Reload na revisão | Não confirma sozinho nem duplica reserva | Mutável |

#### 6.10.6 Telemedicina - 20 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| TEL-V001 | P0 | TEL-001 | Queixa com 9 caracteres | Avanço bloqueado | Leitura |
| TEL-V002 | P1 | TEL-001 | Queixa com 10 caracteres | Limite inferior aceito com demais campos válidos | Mutável |
| TEL-V003 | P1 | TEL-001 | Queixa com mais de 500 caracteres | Limite bloqueado/validado | Leitura |
| TEL-V004 | P0 | TEL-001 | Sem duração | Avanço bloqueado | Leitura |
| TEL-V005 | P0 | TEL-001 | Sintoma de emergência | Alerta de pronto-socorro | Mutável |
| TEL-V006 | P0 | TEL-001 | Emergência sem confirmar risco | Não entra na fila | Mutável |
| TEL-V007 | P1 | TEL-001 | Ação `Ver Pronto-Socorro` | Abre Rede no contexto adequado | Mutável |
| TEL-V008 | P0 | TEL-001 | Termo não aceito | Entrada na fila bloqueada | Mutável |
| TEL-V009 | P0 | TEL-001 | Termo aceito | Posição e estimativa aparecem | Mutável |
| TEL-V010 | P1 | TEL-001 | Recarregar enquanto está na fila | Sessão é recuperada, sem fila duplicada | Mutável |
| TEL-V011 | P0 | TEL-001 | Sair e cancelar confirmação | Mantém posição/sessão | Mutável |
| TEL-V012 | P0 | TEL-001 | Sair e confirmar | Libera posição e volta ao hub | Mutável |
| TEL-V013 | P0 | TEL-002 | Operador inicia atendimento | Sala abre por atualização ao vivo | Reset |
| TEL-V014 | P0 | TEL-002 | Operador encerra com receita | Resumo e documento em Minha Saúde | Reset |
| TEL-V015 | P1 | TEL-002 | Atualização SSE interrompida | Estado recuperável após reload; sem sessão duplicada | Especial |
| TEL-V016 | P0 | TEL-003 | Agendar teleconsulta válida | Protocolo e lista de próximas | Mutável |
| TEL-V017 | P1 | TEL-003 | Avançar sem especialidade | Etapa bloqueada | Leitura |
| TEL-V018 | P1 | TEL-003 | Avançar sem horário | Etapa bloqueada | Leitura |
| TEL-V019 | P0 | TEL-003 | Entrar antes da janela de 10 min | Entrada recusada com orientação | Mutável |
| TEL-V020 | P0 | SEC-001 | Dependente acessa sessão de outra pessoa | Não encontrado, sem dados clínicos | Especial |

#### 6.10.7 Minha Saúde, guias e token - 25 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| SAU-V001 | P0 | SAU-001 | Lista de receituários/atestados | Ordem decrescente e tipos identificáveis | Leitura |
| SAU-V002 | P1 | SAU-001 | Receituário válido | Selo e data de validade | Leitura |
| SAU-V003 | P1 | SAU-001 | Receituário expirado, massa especial | Selo Expirado, sem ocultar documento | Especial |
| SAU-V004 | P0 | SAU-001 | Atestado | CID e afastamento; sem validade inventada | Leitura |
| SAU-V005 | P1 | SAU-001 | Pedido de exame | Exames, TUSS e indicação clínica | Leitura |
| SAU-V006 | P0 | SAU-001 | Baixar PDF | Arquivo corresponde ao detalhe | Leitura |
| SAU-V007 | P1 | SAU-001 | Filtro 30/90/365 dias | Resultado respeita intervalo | Leitura |
| SAU-V008 | P1 | SAU-001 | Intervalo personalizado invertido | Validação clara; consulta não executada | Leitura |
| SAU-V009 | P1 | SAU-001 | Filtro sem resultados | Estado vazio orientativo | Leitura |
| SAU-V010 | P0 | SEC-001 | ID de documento de outra pessoa | Não encontrado | Especial |
| SAU-V011 | P0 | SAU-002 | Encaminhamento > Agendar | Especialidade pré-selecionada | Leitura |
| SAU-V012 | P1 | SAU-002 | Voltar ao documento | Contexto do encaminhamento preservado | Leitura |
| GUI-V001 | P0 | GUI-001 | Listar guias de Maria | Status e período corretos | Leitura |
| GUI-V002 | P1 | GUI-001 | Filtrar status | Somente estados selecionados | Leitura |
| GUI-V003 | P1 | GUI-001 | Filtrar período | Datas dentro do intervalo | Leitura |
| GUI-V004 | P0 | GUI-001 | Abrir guia autorizada | Senha e validade | Leitura |
| GUI-V005 | P1 | GUI-001 | Abrir guia negada | Motivo da negativa | Leitura |
| GUI-V006 | P1 | GUI-001 | Autorização expirada, massa especial | Alerta de expiração | Especial |
| GUI-V007 | P0 | GUI-001 | Selecionar Pedro | Estado vazio sem guia de Maria | Leitura |
| GUI-V008 | P0 | GUI-002 | Simulador autoriza guia | Atualizar muda estado e mostra senha | Reset |
| GUI-V009 | P0 | SEC-001 | Maria tenta rota `/api/sim` | `403`, sem executar transição | Especial |
| TOK-V001 | P0 | TOK-001 | Gerar token | Seis dígitos e contagem de 10 min | Mutável |
| TOK-V002 | P1 | TOK-001 | Copiar token | Clipboard igual ao código | Mutável |
| TOK-V003 | P0 | TOK-001 | Gerar novo | Código muda e anterior é invalidado | Mutável |
| TOK-V004 | P1 | TOK-001 | Token expirado, massa/tempo especial | Estado expirado e ação gerar novo | Especial |

#### 6.10.8 Finanças - 24 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| FIN-V001 | P0 | FIN-001 | Aba Em aberto | Abertos e vencidos, sem pagos | Leitura |
| FIN-V002 | P1 | FIN-001 | Boleto vencido | Orientação de atualização | Leitura |
| FIN-V003 | P0 | FIN-001 | Cálculo do vencido | Original + multa + juros = total | Leitura |
| FIN-V004 | P0 | FIN-001 | Abrir boleto | Linha com 47 dígitos | Leitura |
| FIN-V005 | P1 | FIN-001 | Copiar linha | Clipboard idêntico ao exibido | Leitura |
| FIN-V006 | P1 | FIN-001 | Copiar PIX | Código PIX completo e confirmação | Leitura |
| FIN-V007 | P0 | FIN-001 | Baixar 2ª via | PDF do mesmo boleto | Leitura |
| FIN-V008 | P1 | FIN-001 | Aba Pagos | Data de pagamento visível | Leitura |
| FIN-V009 | P0 | FIN-002 | Validar linha real com espaços | Autêntico | Leitura |
| FIN-V010 | P1 | FIN-002 | Linha com 46 dígitos | Erro de formato | Leitura |
| FIN-V011 | P1 | FIN-002 | Linha com 48 dígitos | Erro de formato | Leitura |
| FIN-V012 | P1 | FIN-002 | Letras misturadas | Erro de formato | Leitura |
| FIN-V013 | P0 | FIN-002 | 47 dígitos desconhecidos | Não reconhecido + não pagar | Leitura |
| FIN-V014 | P1 | FIN-003 | Coparticipação últimos 3 meses | Itens e total | Leitura |
| FIN-V015 | P1 | FIN-003 | Filtrar membro | Itens e total apenas desse membro | Leitura |
| FIN-V016 | P1 | FIN-003 | Período personalizado válido | Datas e total coerentes | Leitura |
| FIN-V017 | P1 | FIN-003 | Período invertido | Erro claro; sem total enganoso | Leitura |
| FIN-V018 | P0 | FIN-003 | PDF de IR | Ano e valores correspondem à tela | Leitura |
| FIN-V019 | P0 | FIN-003 | PDF de quitação | Disponível apenas para ano quitado | Leitura |
| FIN-V020 | P0 | FIN-004 | Pedro ativo | Menu Finanças oculto | Leitura |
| FIN-V021 | P0 | FIN-004 | Pedro abre `/financas` | Tela exclusiva do titular, sem valores | Leitura |
| FIN-V022 | P0 | FIN-004 | Pedro usa URL de boleto de Maria | Acesso negado, sem PDF/linha | Especial |
| FIN-V023 | P1 | FIN-001 | Recarregar detalhe | Mesmo boleto, sem erro | Leitura |
| FIN-V024 | P2 | FIN-001 | Valores em 320 px/200% zoom | Dígitos e totais legíveis, sem sobreposição | Leitura |

#### 6.10.9 Reembolso - 34 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| REI-V001 | P0 | REI-001 | Enviar sem aceitar termo | Bloqueado com mensagem de aceite | Leitura |
| REI-V002 | P0 | REI-001 | Consulta válida com recibo | Protocolo `RE-`, estado e previsão | Mutável |
| REI-V003 | P0 | REI-001 | Valor zero | Rejeitado | Leitura |
| REI-V004 | P1 | REI-001 | Valor negativo | Rejeitado | Leitura |
| REI-V005 | P1 | REI-001 | Valor com mais de 2 casas | Normalização explícita ou rejeição; sem centavos ocultos | Leitura |
| REI-V006 | P0 | REI-001 | Data futura | Rejeitada | Leitura |
| REI-V007 | P1 | REI-001 | Data fora do prazo contratual | `Prazo para solicitação expirado` | Especial |
| REI-V008 | P0 | REI-001 | Prestador obrigatório vazio | Rejeitado e campo identificado | Leitura |
| REI-V009 | P1 | REI-001 | Conselho/UF inválidos | Prestador recusado | Leitura |
| REI-V010 | P0 | REI-001 | CPF/CNPJ do prestador inválido | Prestador recusado | Leitura |
| REI-V011 | P0 | REI-001 | Banco/agência/conta ausentes | Conta recusada | Leitura |
| REI-V012 | P0 | REI-001 | Documento obrigatório ausente | Solicitação não criada | Leitura |
| REI-V013 | P0 | REI-002 | Arquivo falso `.pdf` | Conteúdo inválido | Leitura |
| REI-V014 | P1 | REI-002 | Arquivo de 2 MB exatos | Aceito se assinatura válida | Leitura |
| REI-V015 | P1 | REI-002 | Arquivo de 2 MB + 1 byte | Rejeitado | Leitura |
| REI-V016 | P1 | REI-002 | Total de 20 MB exatos | Aceito | Leitura |
| REI-V017 | P1 | REI-002 | Total acima de 20 MB | Novo arquivo rejeitado; anteriores permanecem | Leitura |
| REI-V018 | P1 | REI-002 | Remover anexo | Item sai e total é recalculado | Leitura |
| REI-V019 | P0 | REI-002 | Terapia: soma das sessões diferente do total | Rejeitada | Leitura |
| REI-V020 | P0 | REI-002 | Terapia: soma igual ao total | Pode prosseguir com documentos válidos | Mutável |
| REI-V021 | P1 | REI-002 | Adicionar e remover sessão | Total e lista permanecem consistentes | Leitura |
| REI-V022 | P0 | REI-001 | Duplo clique em enviar | Uma solicitação lógica | Mutável |
| REI-V023 | P0 | REI-003 | Abrir pago seed | `150,00`, `120,00`, glosa `30,00` | Leitura |
| REI-V024 | P1 | REI-003 | Linha do tempo | Ordem cronológica e estados permitidos | Leitura |
| REI-V025 | P0 | REI-003 | Extrato de junho/2026 | Item seed e total `120,00` | Leitura |
| REI-V026 | P1 | REI-003 | Extrato sem pagamentos | Estado/total zero sem itens fantasmas | Leitura |
| REI-V027 | P0 | REI-004 | Prévia de Consulta | `PV-`, `120,00` e disclaimer | Mutável |
| REI-V028 | P0 | REI-004 | Prévia não Consulta sem anexos | Orçamento e pedido exigidos | Leitura |
| REI-V029 | P1 | REI-004 | Prévia com anexos válidos | Criada e listada | Mutável |
| REI-V030 | P0 | REI-004 | Plano sem direito | Gate informativo, sem dados de terceiros | Leitura |
| REI-V031 | P0 | REI-005 | Pendência + novo documento | Volta a Processamento | Especial |
| REI-V032 | P0 | REI-005 | Falha de pagamento + correção bancária | Retoma fluxo e chega a Pago na simulação | Especial |
| REI-V033 | P0 | REI-005 | Pagar solicitação negada | Transição recusada | Especial |
| REI-V034 | P0 | SEC-001 | Abrir reembolso de outro beneficiário por ID | Não encontrado | Especial |

#### 6.10.10 Atendimento - 14 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| ATE-V001 | P0 | ATE-001 | Banner antifraude da Home | Âncora correta | Leitura |
| ATE-V002 | P1 | ATE-001 | Telefones da Central | Dois links `tel:` válidos | Leitura |
| ATE-V003 | P1 | ATE-001 | WhatsApp | `https://wa.me/` em nova aba | Leitura |
| ATE-V004 | P0 | ATE-001 | Link do validador | Abre validador oficial interno | Leitura |
| ATE-V005 | P1 | ATE-002 | FAQ sem filtro | Pelo menos 12 perguntas | Leitura |
| ATE-V006 | P1 | ATE-002 | Busca `reembolso` | Somente resultados relacionados | Leitura |
| ATE-V007 | P1 | ATE-002 | Busca em maiúsculas/sem acento | Mesmo comportamento sem diferença de caso/acento | Leitura |
| ATE-V008 | P1 | ATE-002 | Limpar busca | Lista completa restaurada | Leitura |
| ATE-V009 | P1 | ATE-002 | Termo inexistente | Estado vazio com termo pesquisado | Leitura |
| ATE-V010 | P1 | ATE-002 | Trocar categoria | Somente perguntas da categoria | Leitura |
| ATE-V011 | P1 | ATE-002 | Abrir duas perguntas | Acordeão mantém uma aberta | Leitura |
| ATE-V012 | P1 | ATE-002 | Solicitar Libras dentro do horário | Confirma videochamada em instantes | Mutável |
| ATE-V013 | P1 | ATE-002 | Solicitar Libras fora do horário | Confirma próximo período | Especial |
| ATE-V014 | P2 | ATE-001 | Abrir links externos com teclado | Foco visível e nova aba previsível | Leitura |

#### 6.10.11 Casos transversais - 18 casos

| ID | Pri | Base | Variação | Esperado | Estado |
|---|---|---|---|---|---|
| X-V001 | P0 | SMK-001 | Recarregar cada rota principal | Sessão/rota recuperadas | Leitura |
| X-V002 | P0 | SEC-001 | Alternar beneficiário durante carregamento | Nenhum dado do contexto anterior permanece | Leitura |
| X-V003 | P0 | SEC-001 | URL com UUID inexistente | 404 controlado, sem stack trace | Leitura |
| X-V004 | P0 | SEC-002 | Logout em uma aba, ação em outra | Exige novo login | Leitura |
| X-V005 | P0 | SEC-002 | Resposta 401 durante ação | Tela de sessão expirada, sem loop | Especial |
| X-V006 | P1 | SEC-003 | Nome de arquivo muito longo | Tratado sem quebrar layout/caminho | Leitura |
| X-V007 | P1 | SEC-003 | Nome com espaço e caracteres especiais | Nome seguro; conteúdo continua validado | Leitura |
| X-V008 | P0 | SEC-003 | Executável renomeado | Rejeitado em toda superfície de upload | Leitura |
| X-V009 | P0 | SEC-004 | Duplo envio | Sem duplicação silenciosa | Mutável |
| X-V010 | P1 | SEC-004 | Offline durante envio | Erro e retry explícitos | Mutável |
| X-V011 | P1 | 8.1 | Teclado no fluxo principal | Ordem/foco e ações completos | Leitura |
| X-V012 | P1 | 8.1 | Zoom 200% | Sem perda/sobreposição de conteúdo | Leitura |
| X-V013 | P1 | 8.2 | Largura 320 px | Sem rolagem horizontal global | Leitura |
| X-V014 | P1 | 8.3 | Fast 3G | Loading e botões protegidos | Leitura |
| X-V015 | P0 | SEC-002 | Inspecionar URL/Console/Network | Sem senha/token/dado clínico exposto | Leitura |
| X-V016 | P1 | SMK-001 | Mensagem de erro em qualquer módulo | pt-BR, acionável, sem código técnico cru | Leitura |
| X-V017 | P1 | SMK-001 | Navegar com 10+ ciclos entre áreas | Menu, contexto e memória continuam estáveis | Leitura |
| X-V018 | P1 | 8.4 | Duas abas no mesmo registro | Conflito controlado; sem perda silenciosa | Mutável |

O catálogo contém **241 casos atômicos** além das 42 jornadas guiadas. A contagem é intencional:
um caso atômico valida uma expectativa e pode ser rerodado isoladamente.

## 7. Pacote C - Segurança, privacidade e negativos

Execute após a regressão principal e, quando indicado, em stack limpa.

### QA-SEC-001 - Autorização por pessoa e por papel

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Como Maria, alterne repetidamente Maria/Pedro em Carteirinha, Saúde, Guias e Agendamentos. | Toda lista, detalhe e download acompanha o beneficiário ativo. |
| 2 | Com Pedro ativo, tente usar URL copiada de um detalhe de Maria. | Recurso é negado como não encontrado; nenhum dado de Maria aparece. |
| 3 | Com Pedro ativo, tente `/financas` e URLs de boleto conhecidas. | Acesso negado sem valores, linha digitável ou PDF. |
| 4 | Após logout, use Voltar do navegador. | Conteúdo privado não volta utilizável; o sistema exige login. |
| 5 | Abra uma URL com identificador aleatório em documento, guia, boleto ou reembolso. | Resposta controlada de não encontrado, sem stack trace. |

### QA-SEC-002 - Sessão e dados sensíveis

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Inspecione telas e URLs durante login, reembolso e documentos. | Senha, token e conteúdo do arquivo não aparecem na URL. |
| 2 | Abra Console e Network. | Não há senha/token exposto em mensagens de erro ou corpo desnecessário. |
| 3 | Saia em uma aba e tente ação autenticada em outra. | A sessão inválida é tratada e volta ao login. |
| 4 | Force atualização durante um formulário parcialmente preenchido. | Não ocorre envio duplicado silencioso; perda de dados é clara. |

### QA-SEC-003 - Upload hostil

Repita em foto, pedido de exame e reembolso conforme os limites de cada área:

- arquivo executável renomeado como `.jpg` ou `.pdf`;
- nome longo, espaços e caracteres especiais;
- arquivo vazio;
- conteúdo válido com extensão em maiúsculas;
- arquivo exatamente no limite e um byte acima;
- vários arquivos até ultrapassar o total permitido;
- duplo clique no envio.

Esperado: validação por conteúdo e tamanho, mensagem compreensível, nenhuma execução do arquivo,
nenhum caminho interno do servidor e nenhuma duplicação.

### QA-SEC-004 - Dinheiro e idempotência

| Nº | Ação | Resultado esperado |
|---:|---|---|
| 1 | Dê duplo clique rápido ao confirmar agendamento ou reembolso. | Um único protocolo lógico ou erro controlado; nunca duas cobranças/solicitações silenciosas. |
| 2 | Reenvie a página após sucesso. | Não cria registro adicional automaticamente. |
| 3 | Compare totais de boleto, coparticipação, reembolso e PDF. | Formatação e valores coincidem em lista, detalhe e arquivo. |
| 4 | Teste valor `0`, negativo, muitas casas decimais e extremamente alto. | Valores inválidos são bloqueados; não há arredondamento enganoso. |

## 8. Pacote D - Exploratório e não funcional

### 8.1 Acessibilidade

Execute os fluxos de login, agendamento, boleto e reembolso somente com teclado:

1. Use `Tab` e `Shift+Tab`.
2. Confirme foco visível e ordem lógica.
3. Use `Enter`/`Espaço` em botões, abas, acordeões e seletores.
4. Confirme que diálogos prendem o foco e o devolvem ao fechar.
5. Aumente o zoom para 200%.
6. Verifique se textos, botões e mensagens não se sobrepõem.
7. Use um leitor de tela, se disponível, para conferir rótulos, erros e mudança de estado.

Falhas graves: ação sem nome acessível, teclado preso, foco perdido após erro, conteúdo essencial
invisível a 200%, informação transmitida apenas por cor.

### 8.2 Responsividade

Repita o smoke nas larguras:

| Perfil | Largura sugerida |
|---|---:|
| Celular pequeno | 320 px |
| Celular comum | 390 px |
| Tablet | 768 px |
| Desktop | 1366 px |

Observe menu, tabelas, cartões digitais, formulários longos, abas de reembolso, diálogos e
downloads. Não pode haver rolagem horizontal da página, texto cortado ou botão inacessível.

### 8.3 Performance percebida e resiliência

Use DevTools > Network para testar `Fast 3G` e `Offline`:

- toda chamada demorada deve mostrar carregamento;
- botões de envio devem impedir repetição enquanto processam;
- falha de rede deve mostrar mensagem e opção de tentar novamente;
- ao voltar online, a tentativa deve ser explícita, sem registro duplicado;
- listas grandes devem continuar roláveis e responsivas;
- troca de beneficiário não deve exibir dados antigos como se fossem do novo contexto.

### 8.4 Charters exploratórios

Execute sessões de 20 a 30 minutos e registre notas:

| Charter | Missão |
|---|---|
| Família | Tentar provocar mistura de dados alternando Maria/Pedro durante carregamentos e downloads |
| Jornada interrompida | Recarregar, voltar, avançar e fechar aba em cada etapa de agendamento/reembolso |
| Upload | Combinar tipos, tamanhos, nomes e remoções até encontrar validação inconsistente |
| Dinheiro | Comparar centavos, totais, datas, status e PDFs em todas as telas financeiras |
| Concorrência | Abrir duas abas e tentar reservar o mesmo horário ou alterar o mesmo registro |
| Conteúdo | Procurar texto técnico, tradução ausente, data ambígua e instrução sem próximo passo |
| Privacidade | Procurar CPF, conta, CID ou documento aparecendo além do necessário |

## 9. Automação assistida

### 9.1 Playwright completo

Com a stack E2E já ativa:

```powershell
cd frontend
npm run e2e
```

O resultado esperado é zero falhas. Para observar um arquivo específico:

```powershell
npx playwright test e2e/reembolso.spec.ts --headed --workers=1
```

Uma falha automatizada não substitui reprodução manual, mas deve ser anexada ao defeito.

### 9.2 Gates de engenharia

O responsável técnico, não necessariamente o QA manual, executa:

```powershell
cd backend
.\mvnw.cmd verify "-Dmodulith.diagram.write=true" "-Dopenapi.snapshot.write=true"

cd ..\frontend
npm run lint
npm test
npm run build
```

No PowerShell, as propriedades Maven `-D...` devem permanecer entre aspas.

## 10. Matriz de cobertura

| Área | Casos manuais | Automação E2E | Autoridade |
|---|---|---|---|
| Acesso e segurança | `QA-AUT-*`, `QA-SEC-*` | `primeiro-acesso`, `seguranca-conta` | SPEC-0002, SPEC-0003 |
| Home e plano | `QA-HOM-*`, `QA-PLN-*` | `home`, `meu-plano` | SPEC-0001, SPEC-0005 |
| Notificações | `QA-NOT-*` | `notificacoes` | SPEC-0004 |
| Perfil e legal | `QA-PRF-*`, `QA-AUT-004` | `perfil` | SPEC-0006 |
| Carteirinha | `QA-CAR-*` | `carteirinha` | SPEC-0007 |
| Rede | `QA-RED-*` | `rede` | SPEC-0008 |
| Agendamento | `QA-AGE-*` | `agendamento` | SPEC-0009 |
| Telemedicina | `QA-TEL-*` | `tele` | SPEC-0010, SPEC-0018 |
| Minha Saúde | `QA-SAU-*` | `minha-saude` | SPEC-0011 |
| Guias e token | `QA-GUI-*`, `QA-TOK-*` | `guias` | SPEC-0012, SPEC-0018 |
| Finanças | `QA-FIN-*` | `financas` | SPEC-0013 |
| Atendimento | `QA-ATE-*` | `atendimento` | SPEC-0014 |
| Reembolso | `QA-REI-*` | `reembolso` | SPEC-0015, SPEC-0018 |
| Armazenamento | uploads de Perfil, Agendamento e Reembolso | integração backend | SPEC-0019 |

## 11. Modelo de defeito

```text
Título: [Área] comportamento observado em condição específica
Severidade: Crítica | Alta | Média | Baixa
Ambiente/build:
Caso de origem:
Usuário e beneficiário ativo:
Pré-condições:
Passos para reproduzir:
1.
2.
3.
Resultado observado:
Resultado esperado:
Frequência: sempre | intermitente | uma vez
Evidências:
Impacto no usuário:
Contorno conhecido:
```

Critério de severidade:

| Severidade | Exemplo |
|---|---|
| Crítica | vazamento de outra pessoa, autenticação contornada, valor financeiro incorreto, perda ampla |
| Alta | jornada principal impossível, arquivo perdido, estado irreversível ou autorização indevida |
| Média | função possui contorno, validação inconsistente, acessibilidade relevante |
| Baixa | texto, alinhamento ou melhoria sem impacto funcional significativo |

## 12. Fechamento da execução

Antes de aprovar:

- saúde da aplicação e banco está `UP`;
- Pacote A passou integralmente;
- casos de alto risco alterados no build passaram;
- nenhum defeito crítico ou alto permanece aberto sem aceite formal;
- downloads foram abertos, não apenas iniciados;
- isolamento Maria/Pedro foi comprovado;
- evidências não contêm segredo ou dado pessoal;
- resultados `BLOQUEADO` e `N/A` têm justificativa;
- stack E2E foi encerrada com `npm run e2e:down`.

## 13. Referências de técnica

- ISTQB CTFL 4.0: https://istqb.org/certifications/certified-tester-foundation-level-ctfl-v4-0/
- OWASP Web Security Testing Guide: https://owasp.org/www-project-web-security-testing-guide/
- OWASP API Security Top 10: https://owasp.org/API-Security/editions/2023/en/0x00-header/
- OWASP File Upload Cheat Sheet:
  https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- NIST Cybersecurity Framework 2.0: https://www.nist.gov/cyberframework
- ANPD - direitos do titular:
  https://www.gov.br/anpd/pt-br/canais_atendimento/cidadao-titular-de-dados
