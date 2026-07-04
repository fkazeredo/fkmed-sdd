# Manual do Usuário — FKMed · Portal do Beneficiário

> Artefato vivo em **pt-BR** (idioma do produto). Atualizado via `/manual` ao fechar toda
> fatia com mudança visível ao usuário — a fatia só está "pronta" quando este manual
> reflete a mudança. Versão do produto: **0.2.0** (pré-release; tag pendente do owner).

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

### 3. Meu Plano

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
| 2026-07-04 | 0.2.0 | Primeiro acesso (criação de conta + verificação de e-mail) e login real com conta própria |
| 2026-07-04 | 0.1.0 | Esqueleto funcional: login (dev), shell de navegação e tela Meu Plano |
