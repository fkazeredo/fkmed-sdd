# Manual do Usuário — FKMed · Portal do Beneficiário

> Artefato vivo em **pt-BR** (idioma do produto). Atualizado via `/manual` ao fechar toda
> fatia com mudança visível ao usuário — a fatia só está "pronta" quando este manual
> reflete a mudança. Versão do produto: **0.11.0** (pré-release; tag pendente do owner).

## Sobre o portal

O FKMed é o portal web do beneficiário do plano de saúde: identifique-se na rede
(carteirinha digital e token de atendimento), encontre atendimento (rede credenciada,
agendamento, telemedicina), acompanhe suas guias, cuide das finanças do contrato
(boletos, coparticipação, IR) e solicite e acompanhe reembolsos.

## Capítulos

### 1. Primeiro acesso (criar sua conta)

Se você é beneficiário e ainda não tem uma conta, clique em **Primeiro acesso? Crie sua
conta** na tela de login (ou acesse `/primeiro-acesso`). O cadastro tem três passos:

1. **Confirme seus dados** — informe **CPF**, **número da carteirinha** e **data de
   nascimento**. Os dados precisam coincidir exatamente com os do seu plano; caso contrário,
   uma única mensagem genérica ("Não encontramos seus dados de cadastro…") é exibida, sem
   indicar qual campo divergiu. Dependentes **menores de 18 anos** não criam conta própria —
   são atendidos pela conta do titular. Se já existir uma conta para o beneficiário, o portal
   orienta a **fazer login** ou recuperar a senha.
2. **Crie seu acesso** — informe seu **e-mail** e uma **senha** (mínimo de 8 caracteres, com
   ao menos 1 letra e 1 número, diferente do e-mail; o botão **Mostrar/Ocultar** permite
   conferir a senha). Marque o aceite dos **Termos de Uso** e da **Política de Privacidade** e
   clique em **Criar conta**.
3. **Verifique seu e-mail** — enviamos um link de verificação, **válido por 24 horas**. Abra o
   link para ativar sua conta. Na página **Verificação de e-mail**, se o link estiver expirado
   ou inválido, você pode **solicitar um novo link** informando seu e-mail.

### 2. Entrar no portal

Acesse o portal pelo navegador. Se você ainda não estiver autenticado, será levado à tela
**Entrar no FKMed**: informe seu **E-mail** e sua **Senha** e clique em **Entrar**. Em caso de
credenciais incorretas, a mensagem neutra "Dados de acesso inválidos." é exibida (sem revelar
se o e-mail existe). Se a conta ainda **não teve o e-mail verificado**, o login é recusado com
orientação para verificar o e-mail — você pode solicitar um novo link.

Para sair, use o botão **Sair** no canto superior direito.

### 3. Início (a Home)

Ao entrar, o portal abre a **Home**: o **cartão do beneficiário ativo** (saudação "Olá, {NOME}",
plano e número da carteirinha), o **Acesso Rápido** (atalhos para as jornadas — os módulos ainda
não liberados aparecem como "em breve", e **Reconhecimento Facial** abre o aviso de uso pelo
aplicativo móvel), **banners** da operadora (rotação automática, com pausa ao passar o mouse) e
**avisos** (expansíveis, um por vez, com destaque visual para alertas). Trocar o beneficiário
ativo no topo atualiza o cartão na hora. Use **Início** no menu para voltar à Home.

### 4. Meu Plano

Acessível pelo menu lateral, a tela **Meu Plano** mostra os dados do seu contrato, vindos
diretamente da operadora:

- **Nome do plano** e **Registro ANS**;
- **Abrangência** (ex.: Estadual);
- **Coparticipação** e **Reembolso** (Sim/Não);
- **Adicionais** contratados (ex.: Urg/emerg Nacional Hr — Assistência);
- **Integrantes do plano**: nome, papel (Titular/Dependente) e número da carteirinha de
  cada membro da família coberta.

Enquanto os dados carregam, a tela exibe "Carregando…". Se houver falha de comunicação, uma
mensagem de erro é mostrada com o botão **Tentar novamente**.

### 5. Segurança da conta

No menu lateral, a tela **Segurança** reúne o cuidado com o seu acesso:

- **Trocar a senha** — informe a **senha atual**, a **nova senha** (mesma política do cadastro) e
  a confirmação. Seu **e-mail de login** aparece apenas para leitura. Há também um aviso de que o
  **reconhecimento facial** está disponível no aplicativo móvel.
- **Esqueci minha senha** — em **Esqueci minha senha** (`/recuperar-senha`), informe seu e-mail. A
  resposta é sempre neutra ("se o e-mail estiver cadastrado, enviaremos instruções"); quando há
  conta, enviamos um link **válido por 30 minutos e de uso único**.
- **Redefinir a senha** — pelo link do e-mail, defina a nova senha. Ao concluir, **as demais
  sessões** abertas são encerradas por segurança.
- **Bloqueio temporário** — após **5 tentativas** de senha incorreta, a conta é **bloqueada por 15
  minutos**; nesse período o login é recusado mesmo com a senha correta.
- **Sessão expirada** — após um tempo de inatividade a sessão expira e você é levado ao login com o
  aviso **"Sua sessão expirou"**, retornando à tela em que estava assim que autenticar. Marcar
  **Manter conectado** no login mantém a sessão por até 7 dias.

### 6. Trocar o beneficiário ativo

No topo do portal há o seletor de **beneficiário ativo**. O **titular** vê a si mesmo e aos seus
**dependentes**; um dependente vê apenas a si. Ao trocar o beneficiário ativo, as telas passam a
operar no contexto do beneficiário escolhido (por exemplo, o cartão do beneficiário na Home). O
acesso é sempre validado pelo servidor: dados de alguém fora do seu núcleo familiar não são
acessíveis.

### 7. Notificações

No topo do portal, o **sino** mostra quantas notificações você tem **não lidas**. Clique nele para
abrir a **Central de Notificações**: as mais recentes primeiro, com estado lido/não lido e um link
direto para o assunto de cada uma. Você pode **marcar como lida** uma a uma ou **marcar todas como
lidas** — o contador do sino atualiza na hora. Em **Preferências de notificação** você escolhe, por
tipo de evento, se também quer receber um **e-mail**; a notificação no aplicativo é sempre enviada.
Tipos de segurança/conta (por exemplo, troca de senha ou alteração dos dados de contato) são
**obrigatórios** e não podem ter o e-mail desativado. Nenhuma notificação traz dados sensíveis
completos (CPF, CNS ou dados bancários).

### 8. Perfil e conta

No menu de **Perfil** você cuida da sua conta:

- **Alterar Foto** — envie uma foto (JPG ou PNG, até **5 MB**) com **recorte quadrado**; ela vale
  para o beneficiário e passa a aparecer em todo o portal (Home, perfil) **sem novo login**.
  **Remover foto** volta ao avatar padrão. O titular pode alterar a foto de um dependente pelo
  seletor de beneficiário ativo.
- **Alterar Cadastro** — seus **dados de contrato** (nome, CPF, data de nascimento, número da
  carteirinha) são somente leitura ("Para alterar, procure os canais de atendimento"). Você edita
  seus **dados de contato e endereço** — **e-mail de contato** e **celular** são obrigatórios. O
  salvamento é **parcial** (só o que mudou) e confirmado. Ao trocar o e-mail de contato, um **aviso
  de segurança** é enviado ao endereço **antigo e ao novo**.
- **Comunicado de privacidade** e **Termos de uso** — leia os documentos com **versão e data**.
  Quando uma nova versão é publicada, o portal **intercepta a navegação** e pede o **"Li e aceito"**
  antes de continuar (apenas **Sair** fica disponível).
- **Segurança**, **Central de Libras** e **Perguntas Frequentes** — atalhos (alguns "em breve").
- **Sair** — encerra a sessão (com confirmação). A **versão do produto** aparece ao lado do Sair.

### 9. Carteirinha digital

A **Carteirinha** mostra o cartão do beneficiário ativo — identidade visual, nome, plano, número e
selo de abrangência — e a **ficha de dados** (CNS, Registro ANS, abrangência e adicionais). Use
**Copiar número** para copiar os 9 dígitos e **Salvar Carteirinha** para baixar um **PDF** (formato
cartão em folha A4) e levar à recepção. **Minhas Carteirinhas** lista a carteirinha de cada
beneficiário a que você tem acesso; selecionar uma abre o cartão desse beneficiário. O **CNS**
aparece por extenso **apenas** nesta tela e no PDF; em qualquer outro lugar do portal ele é
mascarado. Um beneficiário **inativo** mostra o aviso "carteirinha indisponível".

### 10. Rede Credenciada (encontrar prestadores)

Em **Rede Credenciada** você encontra os prestadores da rede — dentro da **cobertura do seu plano**
e apenas os que estão **ativos**. O hub abre em **Busca de rede** (os cartões **Agendamento**,
**Telemedicina** e **Minha Saúde** levam às demais jornadas). Há duas formas de buscar:

- **Buscar por localidade** (funil): escolha **Estado → Município → Bairro** (o bairro é opcional —
  "Todos"), depois **O que deseja buscar?** (o tipo de serviço) e, quando o tipo pedir, a
  **especialidade**. As opções vão sendo montadas a partir dos prestadores realmente disponíveis, então
  você só vê localidades e especialidades que existem na rede.
- **Buscar por nome**: digite ao menos **3 caracteres** do nome do prestador (dá para **filtrar por
  município**).

A lista **Prestadores encontrados** mostra a data de **referência** e permite **alterar localidade** ou
**especialidade**. Ao abrir um prestador, o **Detalhe** traz **Especialidades**, **Endereço**,
**Telefone** e **Selos** de qualificação, com **Traçar rota** (abre o mapa) e **Copiar endereço**. Um
prestador que saiu da rede mostra "**Prestador indisponível**".

### 11. Agendamento (consultas e exames)

Em **Agendamento** você marca, cancela e remarca **consultas** e **exames** nas unidades próprias da
operadora. O hub tem **Agendar Consulta**, **Agendar Exame**, **Meus Agendamentos** e **Telemedicina**.

- **Agendar Consulta** (4 etapas): **Especialidade → Unidade → Data e horário → Revisão** e
  **Confirmar agendamento**. Ao confirmar, aparece o seu **protocolo**.
- **Agendar Exame** (5 etapas): **Exame → Pedido médico → Unidade → Data e horário → Revisão**. O
  **pedido médico** é obrigatório — anexe um **JPG, PNG ou PDF de até 5 MB**.
- Os horários respeitam a **antecedência mínima de 2 horas** e a janela dos **próximos 30 dias**. Como
  a vaga é real, se alguém confirmar o último lugar antes de você o portal avisa "**O horário acabou de
  ser preenchido. Escolha outro.**".

**Meus Agendamentos** separa **Próximos** e **Histórico** (com filtro por **beneficiário**) e mostra o
**status** de cada um — *Agendado, Reagendado, Cancelado, Realizado*. Você pode **Cancelar** (libera o
horário; motivo opcional; não é possível após o horário de início) e **Reagendar** (escolhe nova data e
horário **mantendo o protocolo**). A cada confirmação, cancelamento ou remarcação você recebe um aviso
na **central de notificações** e, conforme suas preferências, por **e-mail**.

### 12. Telemedicina

Em **Telemedicina** você fala com um profissional à distância, de duas formas.

- **Pronto Atendimento** (24h): faça a **triagem** — informe a queixa (10 a 500 caracteres), marque os
  **sintomas** (e a duração) e, se marcar um sintoma de **emergência**, o portal alerta e orienta a
  procurar um **pronto-socorro 24h**, deixando você decidir se prossegue. Aceite o **termo de
  teleatendimento** e **entre na fila**: a tela mostra a sua **posição e a estimativa de espera
  atualizando sozinha** (sem recarregar). Você pode **Sair da fila** a qualquer momento. Quando chega a
  sua vez, a **sala** abre mostrando o profissional (nome e CRM) e o tempo de atendimento correndo — sem
  vídeo neste momento. Ao encerrar, você vê um **resumo** com as orientações e os **documentos emitidos**,
  com o link **"Ver em Minha Saúde"**. Você recebe avisos na central de notificações (e por e-mail).
- **Teleconsulta agendada**: escolha a **especialidade**, a **data e horário** da agenda de telemedicina
  e confirme — vira um agendamento com o selo **Telemedicina** (mesmo protocolo/cancelar/remarcar das
  consultas). O botão **"Entrar na consulta"** habilita **10 minutos antes** do horário até o fim.

Só **um** atendimento por vez: se você já está na fila e abre outro Pronto Atendimento, o portal retoma o
atendimento existente, mantendo a sua posição.

### 13. Minha Saúde (seus documentos)

Em **Minha Saúde** ficam os seus documentos clínicos, em 3 categorias: **Solicitação de Exames**,
**Encaminhamentos** e **Receituários/Atestados**. Eles nascem nos seus atendimentos (por exemplo, no
encerramento de uma telemedicina) e aparecem **na hora**. Em cada lista você filtra por **beneficiário** e
**período** (30/90/365 dias ou um intervalo personalizado); cada card mostra o título, o profissional e
CRM, a data e um **selo de validade** ("Válido até…" ou "Expirado" — um documento expirado continua
disponível para baixar). O **detalhe** traz o cabeçalho e o conteúdo por tipo — exames solicitados (com
código TUSS), medicações (posologia e orientação), encaminhamento (especialidade e motivo) ou atestado
(período de afastamento, **CID** e observações) — e o botão **Baixar PDF**. Num **encaminhamento**, o
botão **"Agendar consulta"** abre o assistente de agendamento já com a especialidade escolhida. Você só vê
os documentos dos beneficiários a que tem acesso; o acesso do titular a um documento de dependente é
registrado.

### 14. Guias e Token

Em **Guias e Token** você acompanha as suas **guias de autorização** e gera o **token de atendimento**.
As guias são abertas pelo prestador junto à operadora — você apenas **acompanha**. A lista mostra, da
mais recente para a mais antiga, o **número**, o **tipo** (Consulta · SP/SADT–Exames · Internação), o
prestador solicitante, a data e um **selo de situação** (em análise, autorizada, parcialmente autorizada,
negada, cancelada ou executada); dá para **filtrar** por situação e por período e **atualizar** a
qualquer momento. Se você não tem guias, a tela orienta o que fazer — nunca fica em branco. No **detalhe**
aparecem os itens (com código TUSS), e, quando a guia está **autorizada**, a **senha de autorização e a
validade**; quando **negada**, o **motivo**; uma autorização vencida mostra um aviso para procurar o
prestador/os canais. Toda mudança de situação gera uma **notificação** para você.

O **token de atendimento** é um código antifraude de **6 dígitos** que você apresenta na recepção. Ele
vale **10 minutos** (com contagem regressiva na tela), e só existe **um token válido por vez** — ao gerar
um novo, o anterior deixa de valer na hora. Você pode **copiar** o código com um toque e, quando ele
expira, a tela mostra "Token expirado" e oferece gerar outro. O titular pode gerar o token de um
dependente pelo seletor de beneficiário (esse acesso fica registrado).

### 15. Finanças (boletos, coparticipação, IR e quitação)

**Finanças** reúne o financeiro do seu contrato e é **exclusiva do titular** — para dependentes os
cartões ficam ocultos e o acesso direto mostra uma tela de aviso. Em **Boletos** você tem duas abas:
**Em aberto** (o do mês e os vencidos, em destaque) e **Pagos**. Um boleto **vencido** mostra o
**valor atualizado** — valor original + **multa de 2%** + **juros de mora de 1% ao mês** (proporcional
aos dias) — com a orientação para atualizar o boleto pelos canais (o portal não faz pagamento online).
No detalhe você pode **copiar a linha digitável** (os 47 dígitos), **copiar o código PIX** e **baixar
a 2ª via em PDF** (um boleto pago vem com a marca **PAGO**).

O **validador de boleto** é o seu antifraude: cole a linha e ele confirma **Boleto autêntico**
(com competência e valor) ou avisa **"Boleto não reconhecido — não realize o pagamento"** e orienta a
procurar os canais oficiais. O **Extrato de coparticipação** lista suas utilizações com filtros por
período e por beneficiário, somando o **total do período**. Em **Imposto de Renda** você baixa o
informe anual (12 meses + total) dos anos com pagamento, e em **Quitação** a **declaração anual de
quitação de débitos (Lei 12.007)** é oferecida para os anos totalmente pagos.

### 16. Atendimento (canais, antifraude e FAQ)

**Atendimento** reúne todos os canais oficiais de contato com a operadora: **Central de
Atendimento 24h** (números para capitais e demais localidades, toque para ligar), **WhatsApp
oficial** (abre a conversa em uma nova aba), **Ouvidoria** e **ANS**, cada um com seu horário
quando aplicável. A seção **Alerta de golpe!** explica que a operadora nunca solicita dados ou
pagamentos por WhatsApp e reforça as boas práticas — nunca compartilhar senha/token, validar o
boleto antes de pagar (com atalho para o validador de Finanças) e usar somente os canais desta
página; é para essa seção que o banner de alerta da tela Início leva diretamente.

Em **Perguntas frequentes** você busca por palavra-chave (em tempo real, sem diferenciar
maiúsculas/acentos) e filtra por categoria — Reembolso, Carteirinha, Agendamento, Telemedicina,
Boletos ou Rede; cada pergunta abre em um acordeão que fecha a anterior automaticamente. Em
**Central de Libras** você confere o horário de atendimento em Libras e clica em **"Solicitar
atendimento em Libras"**: dentro do horário, a equipe inicia a videochamada em instantes; fora do
horário, a solicitação é registrada e você é avisado para o próximo período de atendimento.

## Histórico de atualizações

| Data | Versão | Mudança |
|---|---|---|
| 2026-07-07 | 0.11.0 | Plano e finanças (fecha a Fase 5): Atendimento — canais oficiais (Central 24h, WhatsApp, Ouvidoria, ANS), seção antifraude (destino do banner de alerta da Início) e FAQ pesquisável por categoria; Central de Libras com registro do pedido e confirmação por horário |
| 2026-07-06 | 0.10.0 | Plano e finanças: Finanças (titular) — boletos (abas em aberto/pagos, valor atualizado com multa+juros no vencido, copiar linha/PIX, 2ª via PDF com marca PAGO), validador antifraude, extrato de coparticipação, informe de IR e declaração de quitação Lei 12.007 |
| 2026-07-06 | 0.9.0 | Plano e finanças (início): Guias e Token — acompanhamento das guias de autorização (lista, filtros, detalhe com senha/validade ou motivo de negativa, notificação de mudança) e geração do token de atendimento de 6 dígitos (validade 10 min, único válido, copiar, renovar) |
| 2026-07-06 | 0.8.0 | Cuidado digital: Telemedicina (Pronto Atendimento com fila ao vivo, sala e encerramento; teleconsulta agendada) e Minha Saúde (documentos clínicos com filtros, detalhe, PDF e "agendar" a partir de encaminhamento) — fecha a Fase 4 |
| 2026-07-05 | 0.7.0 | Encontrar atendimento: Rede Credenciada (busca por localidade/nome, detalhe do prestador) e Agendamento de consultas/exames (vaga real, protocolo, cancelar/remarcar, avisos) — fecha a Fase 3 |
| 2026-07-05 | 0.6.0 | Minha conta e identificação: central de notificações (sino), perfil (foto, cadastro, termos versionados, sair) e carteirinha digital com PDF — fecha a Fase 2 |
| 2026-07-05 | 0.5.0 | Home (Início): cartão do beneficiário ativo, Acesso Rápido, banners e avisos — fecha a Fase 1 |
| 2026-07-04 | 0.4.0 | Beneficiário ativo: seletor no topo do portal com escopo familiar (titular vê dependentes) |
| 2026-07-04 | 0.3.0 | Segurança da conta: bloqueio, recuperação e troca de senha, sessão e tela Segurança |
| 2026-07-04 | 0.2.0 | Primeiro acesso (criação de conta + verificação de e-mail) e login real com conta própria |
| 2026-07-04 | 0.1.0 | Esqueleto funcional: login (dev), shell de navegação e tela Meu Plano |
