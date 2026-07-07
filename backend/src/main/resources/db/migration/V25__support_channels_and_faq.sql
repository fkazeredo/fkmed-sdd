-- SPEC-0014 (Canais de Atendimento e FAQ), Phase 5 — closes the phase: the domain.support module.
-- support_channel holds the operator-managed contact cards (BR1/BR2, no hardcoded contact
-- anywhere in the product); sublabel differentiates same-type entries (Central 24h has a
-- capitals number AND an other-localities number under one card). support_antifraud is the
-- single-row antifraud section content (BR3). faq_entry holds the searchable FAQ (BR5/BR6).
-- libras_request records each "Solicitar atendimento em Libras" (BR4); ATTENDED is a future
-- operator-side transition never set in this POC (the videocall itself is out of scope).
--
-- OQ1 (owner-decided): every phone/URL/hours value seeded below is a FICTITIOUS placeholder,
-- swappable by a future content migration once the owner provides the definitive contacts and
-- the Central de Libras operating hours (the same window is enforced in code by
-- com.fkmed.domain.support.LibrasHours — keep both in sync if this seed changes).

create table support_channel (
    id uuid primary key,
    type varchar(20) not null,
    label varchar(120) not null,
    sublabel varchar(120),
    value varchar(160) not null,
    hours varchar(160),
    display_order int not null
);

create index idx_support_channel_display_order on support_channel (display_order);

create table support_antifraud (
    id uuid primary key,
    title varchar(160) not null,
    message text not null
);

create table faq_entry (
    id uuid primary key,
    category varchar(20) not null,
    question varchar(200) not null,
    answer text not null,
    display_order int not null,
    active boolean not null default true
);

create index idx_faq_entry_category on faq_entry (category);

create table libras_request (
    id uuid primary key,
    beneficiary_id uuid not null,
    requested_at timestamptz not null,
    situation varchar(20) not null
);

create index idx_libras_request_beneficiary on libras_request (beneficiary_id);

-- BR1 seed: Central 24h (capitals + other localities), WhatsApp oficial, Ouvidoria, ANS.
insert into support_channel (id, type, label, sublabel, value, hours, display_order) values
    ('5c000000-0000-4000-8000-000000000001', 'CENTRAL', 'Central de Atendimento 24h', 'Capitais', '4004-1234', null, 1),
    ('5c000000-0000-4000-8000-000000000002', 'CENTRAL', 'Central de Atendimento 24h', 'Demais localidades', '0800 123 4567', null, 2),
    ('5c000000-0000-4000-8000-000000000003', 'WHATSAPP', 'WhatsApp oficial', null, '+55 11 91234-5678', null, 3),
    ('5c000000-0000-4000-8000-000000000004', 'OUVIDORIA', 'Ouvidoria', null, '0800 765 4321', 'Seg. a sex., 8h às 18h', 4),
    ('5c000000-0000-4000-8000-000000000005', 'ANS', 'ANS — Agência Nacional de Saúde Suplementar', null, '0800 000 0000', 'Seg. a sex., 8h às 20h; sáb., 8h às 14h', 5);

-- BR3 seed: the antifraud section (destination of the Home fraud banner — SPEC-0005 BR9, V8).
insert into support_antifraud (id, title, message) values
    ('5a000000-0000-4000-8000-000000000001', 'Alerta de golpe!',
     'A operadora não solicita dados ou pagamentos por WhatsApp.');

-- BR6 seed: >= 12 FAQ entries across the 6 categories, >= 3 Reembolso aligned with SPEC-0015..17
-- (12-month deadline, per-expense-type documentation, non-binding preview).
insert into faq_entry (id, category, question, answer, display_order, active) values
    ('5f000000-0000-4000-8000-000000000001', 'REEMBOLSO',
     'Qual é o prazo para pedir reembolso?',
     'Você tem até 12 meses a partir da data da despesa para solicitar o reembolso.', 1, true),
    ('5f000000-0000-4000-8000-000000000002', 'REEMBOLSO',
     'Quais documentos preciso anexar no pedido de reembolso?',
     'A documentação exigida varia por tipo de despesa (consulta, exame, terapia...) — o '
     || 'formulário de solicitação lista os documentos necessários para cada tipo antes do envio.',
     2, true),
    ('5f000000-0000-4000-8000-000000000003', 'REEMBOLSO',
     'A simulação de reembolso é um valor garantido?',
     'Não. A prévia de reembolso é uma estimativa não vinculante — o valor final é definido '
     || 'somente após a análise completa do pedido.', 3, true),
    ('5f000000-0000-4000-8000-000000000004', 'REEMBOLSO',
     'Como acompanho o andamento do meu pedido de reembolso?',
     'A tela de Reembolso mostra o status atual de cada pedido: em análise, pendência de '
     || 'documento, aprovado (com eventual glosa) ou pago.', 4, true),
    ('5f000000-0000-4000-8000-000000000005', 'CARTEIRINHA',
     'Como acesso minha carteirinha digital?',
     'A carteirinha digital fica disponível na área "Carteirinha" do portal, com PDF para '
     || 'download quando necessário.', 5, true),
    ('5f000000-0000-4000-8000-000000000006', 'CARTEIRINHA',
     'Posso ver a carteirinha de um dependente?',
     'Sim — o titular pode visualizar a carteirinha de qualquer dependente ativo do contrato '
     || 'trocando o beneficiário ativo no seletor do portal.', 6, true),
    ('5f000000-0000-4000-8000-000000000007', 'AGENDAMENTO',
     'Com quanta antecedência posso agendar uma consulta ou exame?',
     'O agendamento pode ser feito com até 30 dias de horizonte e no mínimo 2 horas antes do '
     || 'horário desejado.', 7, true),
    ('5f000000-0000-4000-8000-000000000008', 'AGENDAMENTO',
     'Como cancelo ou reagendo um atendimento marcado?',
     'Acesse "Meus Agendamentos" e use as opções de cancelar ou reagendar diretamente no card '
     || 'do atendimento.', 8, true),
    ('5f000000-0000-4000-8000-000000000009', 'TELEMEDICINA',
     'Como funciona a fila do Pronto Atendimento por telemedicina?',
     'Após a triagem, você entra em uma fila de espera visível na tela; ao ser chamado, a sala '
     || 'de atendimento abre automaticamente.', 9, true),
    ('5f000000-0000-4000-8000-000000000010', 'TELEMEDICINA',
     'Onde encontro a receita ou atestado emitido na teleconsulta?',
     'Documentos emitidos durante o atendimento (receitas, atestados, encaminhamentos) ficam '
     || 'disponíveis em "Minha Saúde" logo após o encerramento.', 10, true),
    ('5f000000-0000-4000-8000-000000000011', 'BOLETOS',
     'Como é calculado o valor atualizado de um boleto em atraso?',
     'O valor em atraso soma multa de 2% mais juros de mora de 1% ao mês, calculados dia a dia '
     || 'sobre o valor original.', 11, true),
    ('5f000000-0000-4000-8000-000000000012', 'BOLETOS',
     'Como confirmo que um boleto recebido é autêntico?',
     'Use o validador antifraude em Finanças: cole a linha digitável de 47 dígitos e o sistema '
     || 'informa se o boleto é autêntico ou não reconhecido — nunca solicite pagamento fora dos '
     || 'canais oficiais.', 12, true),
    ('5f000000-0000-4000-8000-000000000013', 'REDE',
     'Como encontro um prestador coberto pelo meu plano?',
     'Use a busca de rede por localidade + especialidade ou por nome; os resultados já são '
     || 'filtrados pela cobertura do seu plano.', 13, true),
    ('5f000000-0000-4000-8000-000000000014', 'REDE',
     'A busca de rede mostra prestadores fora da minha cobertura?',
     'Não — a busca é restrita à cobertura contratual do seu plano (UF/abrangência), '
     || 'garantindo que só apareçam prestadores realmente elegíveis.', 14, true);
