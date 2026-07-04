# Manual do Usuário — FKMed · Portal do Beneficiário

> Artefato vivo em **pt-BR** (idioma do produto). Atualizado via `/manual` ao fechar toda
> fatia com mudança visível ao usuário — a fatia só está "pronta" quando este manual
> reflete a mudança. Versão do produto: **0.1.0** (pré-release; tag pendente do owner).

## Sobre o portal

O FKMed é o portal web do beneficiário do plano de saúde: identifique-se na rede
(carteirinha digital e token de atendimento), encontre atendimento (rede credenciada,
agendamento, telemedicina), acompanhe suas guias, cuide das finanças do contrato
(boletos, coparticipação, IR) e solicite e acompanhe reembolsos.

## Capítulos

### 1. Entrar no portal

Acesse o portal pelo navegador. Se você ainda não estiver autenticado, será levado à tela
**Entrar no FKMed**: informe **Usuário** e **Senha** e clique em **Entrar**. Em caso de
credenciais incorretas, a mensagem "Usuário ou senha inválidos." é exibida e você pode
tentar novamente.

> Fase atual (esqueleto): o acesso usa um login de desenvolvimento vinculado à beneficiária
> de referência. O cadastro real de contas (criação, recuperação de senha) chega com a
> funcionalidade "Identidade e Acesso".

Para sair, use o botão **Sair** no canto superior direito.

### 2. Meu Plano

Após entrar, o portal abre a tela **Meu Plano** (também acessível pelo menu lateral). Ela
mostra os dados do seu contrato, vindos diretamente da operadora:

- **Nome do plano** e **Registro ANS**;
- **Abrangência** (ex.: Estadual);
- **Coparticipação** e **Reembolso** (Sim/Não);
- **Adicionais** contratados (ex.: Urg/emerg Nacional Hr — Assistência);
- **Integrantes do plano**: nome, papel (Titular/Dependente) e número da carteirinha de
  cada membro da família coberta.

Enquanto os dados carregam, a tela exibe "Carregando…". Se houver falha de comunicação, uma
mensagem de erro é mostrada com o botão **Tentar novamente**.

## Histórico de atualizações

| Data | Versão | Mudança |
|---|---|---|
| 2026-07-04 | 0.1.0 | Esqueleto funcional: login (dev), shell de navegação e tela Meu Plano |
