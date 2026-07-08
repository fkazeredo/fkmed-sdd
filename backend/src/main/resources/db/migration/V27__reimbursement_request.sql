-- SPEC-0015/0016/0017 (Reimbursement), Phase 6: the domain.reimbursement module.
-- Registries (expense_type, professional_council, bank), the reimbursement table, requests,
-- tracking/timeline, previews and the no-reimbursement E2E fixture live here. The operator-side
-- actions are exposed by the existing application.sim adapter (SPEC-0018).

create table expense_type (
    code varchar(20) primary key,
    name varchar(60) not null
);

insert into expense_type (code, name) values
    ('CONSULTA', 'Consulta medica'),
    ('EXAME', 'Exame'),
    ('TERAPIA', 'Terapia (sessoes)'),
    ('PSICOLOGIA', 'Psicologia (sessoes)'),
    ('HONORARIOS', 'Honorarios medicos (cirurgia)'),
    ('OUTROS', 'Outros');

create table professional_council (
    code varchar(20) primary key,
    name varchar(60) not null
);

insert into professional_council (code, name) values
    ('CRM', 'Conselho Regional de Medicina'),
    ('CRP', 'Conselho Regional de Psicologia'),
    ('CRO', 'Conselho Regional de Odontologia'),
    ('CREFITO', 'Conselho Regional de Fisioterapia e Terapia Ocupacional'),
    ('OUTRO', 'Outro conselho profissional');

create table bank (
    code varchar(10) primary key,
    name varchar(80) not null
);

insert into bank (code, name) values
    ('001', 'Banco do Brasil'),
    ('033', 'Santander'),
    ('104', 'Caixa Economica Federal'),
    ('237', 'Bradesco'),
    ('341', 'Itau Unibanco'),
    ('077', 'Banco Inter'),
    ('260', 'Nubank');

-- SPEC-0016 BR3: reimbursed = min(amount paid, table value) * multiple, per session when
-- per_session=true. OUTROS uses Consulta's amount until the owner defines a distinct value (DL-0025).
create table reimbursement_table (
    expense_type_code varchar(20) primary key references expense_type (code),
    amount numeric(12, 2) not null check (amount > 0),
    per_session boolean not null default false,
    plan_multiple numeric(4, 2) not null default 1.0
);

insert into reimbursement_table (expense_type_code, amount, per_session, plan_multiple) values
    ('CONSULTA', 120.00, false, 1.0),
    ('EXAME', 80.00, false, 1.0),
    ('TERAPIA', 60.00, true, 1.0),
    ('PSICOLOGIA', 60.00, true, 1.0),
    ('HONORARIOS', 900.00, false, 1.0),
    ('OUTROS', 120.00, false, 1.0);

create table reimbursement_adhesion_term (
    id uuid primary key,
    version varchar(10) not null unique,
    published_at date not null,
    body text not null
);

insert into reimbursement_adhesion_term (id, version, published_at, body) values (
    'ea000000-0000-4000-8000-000000000001',
    '1.0',
    current_date - 180,
    'Termo de Adesao ao Reembolso - Livre Escolha. Ao solicitar o reembolso de uma despesa medica '
    || 'realizada fora da rede credenciada, o beneficiario declara que as informacoes prestadas sao '
    || 'verdadeiras e compromete-se a manter os documentos originais pelo prazo de 5 anos, sujeitos '
    || 'a auditoria pela operadora. O reembolso e calculado com base na tabela de referencia do plano '
    || 'vigente na data do atendimento. O prazo para solicitacao e de ate 12 meses contados da data '
    || 'do atendimento.'
);

create table reimbursement_request (
    id uuid primary key,
    protocol varchar(20) not null unique,
    beneficiary_id uuid not null references beneficiary (id),
    expense_type_code varchar(20) not null references expense_type (code),
    care_date date not null,
    amount numeric(12, 2) not null check (amount > 0),
    provider_name varchar(140) not null,
    provider_council_code varchar(20) not null references professional_council (code),
    provider_council_number varchar(10) not null,
    provider_council_uf varchar(2) not null,
    provider_document varchar(14) not null check (provider_document ~ '^\d{11}$|^\d{14}$'),
    provider_specialty varchar(140) not null,
    bank_code varchar(10) not null references bank (code),
    bank_agency varchar(4) not null,
    bank_account varchar(20) not null,
    bank_account_digit varchar(2) not null,
    bank_account_type varchar(10) not null check (bank_account_type in ('CORRENTE', 'POUPANCA')),
    term_version varchar(10) not null,
    term_accepted_at timestamptz not null,
    status varchar(30) not null check (
        status in (
            'EM_ANALISE',
            'PROCESSAMENTO',
            'PENDENTE_DOCUMENTACAO',
            'APROVADO',
            'PAGO',
            'PAGAMENTO_NAO_EFETUADO',
            'NEGADO',
            'CANCELADO'
        )
    ),
    expected_payment_date date not null,
    amount_reimbursed numeric(12, 2),
    glosa_amount numeric(12, 2),
    glosa_reason varchar(300),
    denial_reason varchar(300),
    pendency_description varchar(500),
    pendency_opened_at timestamptz,
    pendency_deadline_at date,
    paid_at timestamptz,
    payment_failed_at timestamptz,
    payment_failure_reason varchar(300),
    idempotency_key varchar(100) not null unique,
    created_by uuid not null,
    created_at timestamptz not null
);

create index idx_reimbursement_request_beneficiary on reimbursement_request (beneficiary_id);
create index idx_reimbursement_request_status on reimbursement_request (status);

create table reimbursement_session_item (
    id uuid primary key,
    request_id uuid not null references reimbursement_request (id),
    session_date date not null,
    amount numeric(12, 2) not null check (amount > 0)
);

create index idx_reimbursement_session_item_request on reimbursement_session_item (request_id);

create table reimbursement_document (
    id uuid primary key,
    request_id uuid not null references reimbursement_request (id),
    category varchar(20) not null check (category in ('RECEIPT', 'MEDICAL_ORDER', 'COMPLEMENTARY')),
    content bytea not null,
    content_type varchar(40) not null,
    file_name varchar(200) not null,
    file_size int not null,
    uploaded_at timestamptz not null
);

create index idx_reimbursement_document_request on reimbursement_document (request_id);

create table reimbursement_timeline_event (
    id uuid primary key,
    request_id uuid not null references reimbursement_request (id),
    occurred_at timestamptz not null,
    status varchar(30) not null,
    description varchar(500)
);

create index idx_reimbursement_timeline_event_request on reimbursement_timeline_event (request_id);

create table reimbursement_preview (
    id uuid primary key,
    protocol varchar(20) not null unique,
    beneficiary_id uuid not null references beneficiary (id),
    expense_type_code varchar(20) not null references expense_type (code),
    situation varchar(20) not null check (situation in ('EM_ANALISE', 'CONCLUIDA')),
    estimated_value numeric(12, 2),
    concluded_at timestamptz,
    created_by uuid not null,
    created_at timestamptz not null
);

create index idx_reimbursement_preview_beneficiary on reimbursement_preview (beneficiary_id);

create table preview_document (
    id uuid primary key,
    preview_id uuid not null references reimbursement_preview (id),
    category varchar(20) not null check (category in ('BUDGET', 'MEDICAL_ORDER')),
    content bytea not null,
    content_type varchar(40) not null,
    file_name varchar(200) not null,
    file_size int not null,
    uploaded_at timestamptz not null
);

create index idx_preview_document_preview on preview_document (preview_id);

insert into notification_event_type (code, description, email_default, mandatory) values
    ('reimbursement.submitted', 'Solicitacao de reembolso enviada', true, false),
    ('reimbursement.pendency-opened', 'Pendencia de documentacao em reembolso', true, false),
    ('reimbursement.pendency-resolved', 'Pendencia de reembolso resolvida', true, false),
    ('reimbursement.approved', 'Reembolso aprovado', true, false),
    ('reimbursement.denied', 'Reembolso negado', true, false),
    ('reimbursement.payment-failed', 'Pagamento de reembolso nao efetuado', true, false),
    ('reimbursement.cancelled', 'Reembolso cancelado', true, false),
    ('preview.concluded', 'Previa de reembolso concluida', true, false);

-- Canonical history seed (SPEC-0016).
insert into reimbursement_request
    (id, protocol, beneficiary_id, expense_type_code, care_date, amount, provider_name,
     provider_council_code, provider_council_number, provider_council_uf, provider_document,
     provider_specialty, bank_code, bank_agency, bank_account, bank_account_digit,
     bank_account_type, term_version, term_accepted_at, status, expected_payment_date,
     amount_reimbursed, glosa_amount, glosa_reason, denial_reason, pendency_description,
     pendency_opened_at, pendency_deadline_at, paid_at, payment_failed_at,
     payment_failure_reason, idempotency_key, created_by, created_at)
values
    ('10000000-0000-4000-8000-000000000001', 'RE-20260601-0001',
     '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 'CONSULTA', date '2026-06-01', 150.00,
     'CLINICA LIVRE ESCOLHA LTDA', 'CRM', '123456', 'RJ', '11222333000181',
     'Clinica medica', '001', '1234', '987654', '1', 'CORRENTE', '1.0',
     timestamptz '2026-06-01 09:00:00-03', 'PAGO', date '2026-06-08',
     120.00, 30.00, 'Valor excede a tabela do plano', null, null, null, null,
     timestamptz '2026-06-08 15:00:00-03', null, null, 'seed-re-20260601-0001',
     'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70', timestamptz '2026-06-01 09:00:00-03'),
    ('10000000-0000-4000-8000-000000000002', 'RE-20260615-0002',
     '9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d', 'EXAME', date '2026-06-15', 300.00,
     'LABORATORIO LIVRE ESCOLHA LTDA', 'CRM', '234567', 'RJ', '22333444000172',
     'Radiologia', '001', '1234', '987654', '1', 'CORRENTE', '1.0',
     timestamptz '2026-06-15 10:00:00-03', 'PENDENTE_DOCUMENTACAO', date '2026-06-29',
     80.00, 220.00, 'Valor excede a tabela do plano', null,
     'Pedido medico ilegivel - reenviar', timestamptz '2026-06-16 10:00:00-03',
     date '2026-07-16', null, null, null, 'seed-re-20260615-0002',
     'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70', timestamptz '2026-06-15 10:00:00-03'),
    ('10000000-0000-4000-8000-000000000003', 'RE-20260620-0003',
     '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 'TERAPIA', date '2026-06-20', 400.00,
     'CENTRO TERAPEUTICO LIVRE ESCOLHA', 'CREFITO', '345678', 'RJ', '33444555000163',
     'Fisioterapia', '001', '1234', '987654', '1', 'CORRENTE', '1.0',
     timestamptz '2026-06-20 11:00:00-03', 'PROCESSAMENTO', date '2026-07-03',
     240.00, 160.00, 'Valor excede a tabela do plano', null, null, null, null,
     null, null, null, 'seed-re-20260620-0003',
     'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70', timestamptz '2026-06-20 11:00:00-03'),
    ('10000000-0000-4000-8000-000000000004', 'RE-20260410-0004',
     '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 'CONSULTA', date '2026-04-10', 200.00,
     'CONSULTORIO LIVRE ESCOLHA', 'CRM', '456789', 'RJ', '44555666000154',
     'Clinica medica', '001', '1234', '987654', '1', 'CORRENTE', '1.0',
     timestamptz '2026-04-10 09:30:00-03', 'NEGADO', date '2026-04-17',
     0.00, null, null, 'Recibo sem identificacao do profissional', null, null, null,
     null, null, null, 'seed-re-20260410-0004',
     'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70', timestamptz '2026-04-10 09:30:00-03'),
    ('10000000-0000-4000-8000-000000000005', 'RE-20260628-0005',
     '3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c', 'HONORARIOS', date '2026-06-28', 2500.00,
     'EQUIPE CIRURGICA LIVRE ESCOLHA', 'CRM', '567890', 'RJ', '55666777000145',
     'Cirurgia geral', '001', '1234', '987654', '1', 'CORRENTE', '1.0',
     timestamptz '2026-06-28 12:00:00-03', 'APROVADO', date '2026-07-10',
     900.00, 1600.00, 'Valor excede a tabela do plano', null, null, null, null,
     null, null, null, 'seed-re-20260628-0005',
     'd4e5f6a7-8b9c-4d0e-9f1a-2b3c4d5e6f70', timestamptz '2026-06-28 12:00:00-03');

insert into reimbursement_session_item (id, request_id, session_date, amount) values
    ('11000000-0000-4000-8000-000000000031', '10000000-0000-4000-8000-000000000003', date '2026-06-01', 100.00),
    ('11000000-0000-4000-8000-000000000032', '10000000-0000-4000-8000-000000000003', date '2026-06-08', 100.00),
    ('11000000-0000-4000-8000-000000000033', '10000000-0000-4000-8000-000000000003', date '2026-06-15', 100.00),
    ('11000000-0000-4000-8000-000000000034', '10000000-0000-4000-8000-000000000003', date '2026-06-20', 100.00);

insert into reimbursement_document
    (id, request_id, category, content, content_type, file_name, file_size, uploaded_at)
select
    gen_random_uuid(),
    request_id,
    category,
    decode('255044462d312e340a', 'hex'),
    'application/pdf',
    file_name,
    9,
    uploaded_at
from (values
    ('10000000-0000-4000-8000-000000000001'::uuid, 'RECEIPT', 'recibo-consulta.pdf', timestamptz '2026-06-01 09:00:00-03'),
    ('10000000-0000-4000-8000-000000000002'::uuid, 'RECEIPT', 'nota-exame.pdf', timestamptz '2026-06-15 10:00:00-03'),
    ('10000000-0000-4000-8000-000000000002'::uuid, 'MEDICAL_ORDER', 'pedido-ilegivel.pdf', timestamptz '2026-06-15 10:00:00-03'),
    ('10000000-0000-4000-8000-000000000003'::uuid, 'RECEIPT', 'recibos-terapia.pdf', timestamptz '2026-06-20 11:00:00-03'),
    ('10000000-0000-4000-8000-000000000003'::uuid, 'MEDICAL_ORDER', 'relatorio-terapia.pdf', timestamptz '2026-06-20 11:00:00-03'),
    ('10000000-0000-4000-8000-000000000004'::uuid, 'RECEIPT', 'recibo-consulta-negado.pdf', timestamptz '2026-04-10 09:30:00-03'),
    ('10000000-0000-4000-8000-000000000005'::uuid, 'RECEIPT', 'honorarios.pdf', timestamptz '2026-06-28 12:00:00-03'),
    ('10000000-0000-4000-8000-000000000005'::uuid, 'MEDICAL_ORDER', 'relatorio-cirurgia.pdf', timestamptz '2026-06-28 12:00:00-03')
) as seed(request_id, category, file_name, uploaded_at);

insert into reimbursement_timeline_event (id, request_id, occurred_at, status, description) values
    ('12000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', timestamptz '2026-06-01 09:00:00-03', 'EM_ANALISE', 'Solicitacao recebida - em analise.'),
    ('12000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000001', timestamptz '2026-06-01 09:01:00-03', 'PROCESSAMENTO', 'Documentacao completa. Reembolso em processamento.'),
    ('12000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000001', timestamptz '2026-06-05 10:00:00-03', 'APROVADO', 'Reembolso aprovado com glosa por limite da tabela.'),
    ('12000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000001', timestamptz '2026-06-08 15:00:00-03', 'PAGO', 'Credito realizado na conta informada.'),
    ('12000000-0000-4000-8000-000000000021', '10000000-0000-4000-8000-000000000002', timestamptz '2026-06-15 10:00:00-03', 'EM_ANALISE', 'Solicitacao recebida - em analise.'),
    ('12000000-0000-4000-8000-000000000022', '10000000-0000-4000-8000-000000000002', timestamptz '2026-06-16 10:00:00-03', 'PENDENTE_DOCUMENTACAO', 'Pedido medico ilegivel - reenviar.'),
    ('12000000-0000-4000-8000-000000000031', '10000000-0000-4000-8000-000000000003', timestamptz '2026-06-20 11:00:00-03', 'EM_ANALISE', 'Solicitacao recebida - em analise.'),
    ('12000000-0000-4000-8000-000000000032', '10000000-0000-4000-8000-000000000003', timestamptz '2026-06-20 11:01:00-03', 'PROCESSAMENTO', 'Documentacao completa. Reembolso em processamento.'),
    ('12000000-0000-4000-8000-000000000041', '10000000-0000-4000-8000-000000000004', timestamptz '2026-04-10 09:30:00-03', 'EM_ANALISE', 'Solicitacao recebida - em analise.'),
    ('12000000-0000-4000-8000-000000000042', '10000000-0000-4000-8000-000000000004', timestamptz '2026-04-12 16:00:00-03', 'NEGADO', 'Recibo sem identificacao do profissional.'),
    ('12000000-0000-4000-8000-000000000051', '10000000-0000-4000-8000-000000000005', timestamptz '2026-06-28 12:00:00-03', 'EM_ANALISE', 'Solicitacao recebida - em analise.'),
    ('12000000-0000-4000-8000-000000000052', '10000000-0000-4000-8000-000000000005', timestamptz '2026-06-28 12:01:00-03', 'PROCESSAMENTO', 'Documentacao completa. Reembolso em processamento.'),
    ('12000000-0000-4000-8000-000000000053', '10000000-0000-4000-8000-000000000005', timestamptz '2026-07-01 10:00:00-03', 'APROVADO', 'Reembolso aprovado com glosa por limite da tabela.');

create extension if not exists pgcrypto;

insert into plan (id, name, ans_registration, coverage, copay, reimbursement, additives) values (
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5b',
    'PLANO MEDICO - BASICO SEM REEMBOLSO',
    '326306',
    'ESTADUAL',
    true,
    false,
    '{}'
);

insert into beneficiary
    (id, plan_id, full_name, cpf, cns, card_number, birth_date, role, titular_id, active,
     contact_email, mobile, cep, street, address_number, neighborhood, city, uf)
values (
    'a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6e',
    'b7e8c9d0-5a4f-4c3e-9b2a-1f0e8d7c6b5b',
    'REEMBOLSO E2E SEM DIREITO',
    '11122233396',
    '700000000000011',
    '009000010',
    date '1990-01-01',
    'TITULAR',
    null,
    true,
    'reembolso.sem.direito@fkmed.local',
    '(21) 98888-0000',
    '20040002',
    'Avenida Rio Branco',
    '156',
    'Centro',
    'Rio de Janeiro',
    'RJ'
);

insert into user_account (id, beneficiary_id, email, password_hash, status, created_at) values (
    'b3c4d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7f',
    'a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6e',
    'reembolso-sem-direito-e2e@fkmed.local',
    '{bcrypt}' || crypt('reembolso12345', gen_salt('bf', 10)),
    'ACTIVE',
    now()
);

insert into term_acceptance (id, account_id, document_type, version, accepted_at) values
    (
        'b4c5d6e7-f8a9-4c0d-1e2f-3a4b5c6d7e8f',
        'b3c4d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7f',
        'TERMS_OF_USE',
        '1.0',
        now()
    ),
    (
        'c5d6e7f8-a9b0-4d1e-2f3a-4b5c6d7e8f90',
        'b3c4d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7f',
        'PRIVACY_POLICY',
        '1.0',
        now()
    );
