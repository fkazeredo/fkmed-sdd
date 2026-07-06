-- SPEC-0014: Service Channels and FAQ — the domain.support module (ADR-0019, 13th module).
-- support_channel (BR1/BR2, read-only registry): the four channel cards. faq_entry (BR5/BR6):
-- category is registry data (domain.support.FaqCategoryCodes), question/answer content.
-- libras_request (BR4): one row per registration; situation always REGISTERED in this phase
-- (ATTENDED is a future operator action, out of scope). Seed numbers/hours are FICTITIOUS
-- placeholders (SPEC-0014 OQ1, owner-approved 2026-07-06) pending real operator content.

create table support_channel (
    id uuid primary key,
    type varchar(20) not null check (type in ('CENTRAL', 'WHATSAPP', 'OUVIDORIA', 'ANS')),
    label varchar(120) not null,
    value varchar(200) not null,
    hours varchar(120),
    display_order int not null
);

create index idx_support_channel_display_order on support_channel (display_order);

create table faq_entry (
    id uuid primary key,
    category varchar(30) not null,
    question varchar(200) not null,
    answer text not null,
    display_order int not null,
    active boolean not null default true
);

create index idx_faq_entry_category on faq_entry (category);
create index idx_faq_entry_display_order on faq_entry (display_order);

create table libras_request (
    id uuid primary key,
    beneficiary_id uuid not null,
    requested_at timestamptz not null,
    situation varchar(20) not null check (situation in ('REGISTERED', 'ATTENDED'))
);

create index idx_libras_request_beneficiary on libras_request (beneficiary_id);

-- BR1 seed: the four channel cards, fictitious placeholder numbers/hours.
insert into support_channel (id, type, label, value, hours, display_order)
values
    (
        'a1000000-0000-4000-8000-000000000001',
        'CENTRAL',
        'Central de Atendimento 24h',
        '0800 123 4567',
        '24 horas, todos os dias',
        1
    ),
    (
        'a1000000-0000-4000-8000-000000000002',
        'WHATSAPP',
        'WhatsApp oficial',
        '+55 11 98765-4321',
        '24 horas, todos os dias',
        2
    ),
    (
        'a1000000-0000-4000-8000-000000000003',
        'OUVIDORIA',
        'Ouvidoria',
        '0800 765 4321',
        'Segunda a sexta, das 8h às 18h',
        3
    ),
    (
        'a1000000-0000-4000-8000-000000000004',
        'ANS',
        'ANS - Agência Nacional de Saúde Suplementar',
        '0800 701 9656',
        'Segunda a sexta, das 8h às 20h',
        4
    );

-- BR6 seed: >= 12 FAQ across the 6 categories, >= 3 Reembolso aligned with SPEC-0015/0017
-- (12-month deadline, documentation per expense type, non-binding preview).
insert into faq_entry (id, category, question, answer, display_order, active)
values
    (
        'a2000000-0000-4000-8000-000000000001',
        'REEMBOLSO',
        'Até quando posso solicitar o reembolso de uma despesa?',
        'Você tem até 12 meses a partir da data do atendimento para solicitar o reembolso. '
            || 'Após esse prazo, a solicitação não pode ser processada.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000002',
        'REEMBOLSO',
        'Quais documentos preciso enviar para pedir reembolso?',
        'A documentação necessária varia conforme o tipo de despesa (consulta, exame, terapia '
            || 'ou internação). O portal mostra a lista completa exigida para o tipo escolhido '
            || 'antes de você enviar o pedido.',
        2,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000003',
        'REEMBOLSO',
        'O valor da prévia de reembolso é garantido?',
        'Não. A prévia é uma estimativa com base nas informações e documentos enviados e nas '
            || 'regras do seu plano. Não representa o valor final nem garante o reembolso — ela '
            || 'não cria nem altera nenhuma solicitação de reembolso.',
        3,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000004',
        'CARTEIRINHA',
        'Como acesso minha carteirinha digital?',
        'Sua carteirinha digital fica disponível no atalho "Carteirinha" da tela inicial, com '
            || 'seus dados de identificação e o número da carteira.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000005',
        'CARTEIRINHA',
        'Posso usar a carteirinha digital em qualquer atendimento?',
        'Sim, a carteirinha digital tem a mesma validade da carteirinha física em toda a rede '
            || 'credenciada.',
        2,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000006',
        'AGENDAMENTO',
        'Como faço para agendar uma consulta pelo portal?',
        'Acesse o atalho "Agendamento", escolha a especialidade e o prestador, e selecione um '
            || 'horário disponível.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000007',
        'AGENDAMENTO',
        'Posso cancelar ou remarcar um agendamento?',
        'Sim, você pode cancelar ou remarcar diretamente na tela de detalhes do agendamento, '
            || 'respeitando o prazo mínimo de antecedência informado no momento da marcação.',
        2,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000008',
        'TELEMEDICINA',
        'Como entro em uma consulta de telemedicina?',
        'Acesse o atalho "Telemedicina" e entre na fila de atendimento; você será chamado '
            || 'assim que um profissional estiver disponível.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000009',
        'TELEMEDICINA',
        'A telemedicina emite atestados e receitas?',
        'Sim, o profissional pode emitir atestado, receita ou pedido de exame ao final do '
            || 'atendimento, disponíveis na sua área de documentos clínicos.',
        2,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000010',
        'BOLETOS',
        'Onde encontro meus boletos de mensalidade?',
        'Os boletos ficam disponíveis na área financeira do portal, com opção de segunda via e '
            || 'validação antes do pagamento.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000011',
        'BOLETOS',
        'Como sei se um boleto recebido por e-mail ou WhatsApp é verdadeiro?',
        'Sempre valide o boleto no validador oficial do portal antes de pagar. A operadora '
            || 'nunca envia cobranças por WhatsApp.',
        2,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000012',
        'REDE',
        'Como encontro um prestador credenciado perto de mim?',
        'Use a busca da Rede Credenciada para filtrar por estado, cidade, especialidade e tipo '
            || 'de atendimento.',
        1,
        true
    ),
    (
        'a2000000-0000-4000-8000-000000000013',
        'REDE',
        'A rede credenciada muda de acordo com o meu plano?',
        'Sim, os prestadores exibidos respeitam a cobertura contratada no seu plano.',
        2,
        true
    );
