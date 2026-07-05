-- SPEC-0005: Home content — banners (validity-windowed, BR6) and notices (BR7) + BR9 seed.
-- Content is operator-loaded via migration in this phase (no CMS yet, BR8/BR9); active/validity
-- filtering happens server-side (domain.content.HomeContent), never by hiding rows here.

create table banner (
    id uuid primary key,
    title varchar(80) not null,
    text varchar(280) not null,
    image varchar(500),
    button_label varchar(60) not null,
    internal_destination varchar(200) not null,
    display_order int not null,
    active boolean not null default true,
    valid_from timestamptz,
    valid_to timestamptz,
    constraint chk_banner_validity_window check (
        valid_from is null or valid_to is null or valid_from <= valid_to
    )
);

create index idx_banner_display_order on banner (display_order);

create table notice (
    id uuid primary key,
    title varchar(160) not null,
    body text not null,
    severity varchar(20) not null check (severity in ('INFORMATIVE', 'ALERT')),
    display_order int not null,
    active boolean not null default true
);

create index idx_notice_display_order on notice (display_order);

-- BR9 seed (operator-authored wording, fictitious POC content). No validity window limit —
-- both banners are open-ended (valid_from/valid_to null).
insert into banner
    (id, title, text, image, button_label, internal_destination, display_order, active,
     valid_from, valid_to)
values
    (
        '2a1b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d',
        'Alerta de golpe!',
        'A operadora não solicita dados pessoais nem pagamentos por WhatsApp. Desconfie de '
            || 'mensagens ou ligações pedindo boletos, senhas ou depósitos.',
        null,
        'Saiba mais',
        '/atendimento#antifraude',
        1,
        true,
        null,
        null
    ),
    (
        '3b2c4d5e-6f7a-4b2c-9d3e-4f5a6b7c8d9e',
        'Valide seu boleto',
        'Antes de pagar, confira a autenticidade do seu boleto no validador oficial da '
            || 'operadora e evite fraudes.',
        null,
        'Validar boleto',
        '/financeiro#validar-boleto',
        2,
        true,
        null,
        null
    );

insert into notice (id, title, body, severity, display_order, active)
values
    (
        '4c3d5e6f-7a8b-4c3d-ae4f-5a6b7c8d9e0f',
        'Instabilidade momentânea da Telemedicina',
        'Estamos passando por uma instabilidade momentânea no atendimento por Telemedicina. '
            || 'Nossas equipes já estão trabalhando na normalização do serviço.',
        'ALERT',
        1,
        true
    ),
    (
        '5d4e6f7a-8b9c-4d4e-bf5a-6b7c8d9e0f1a',
        'Lei Geral de Proteção de Dados Pessoais',
        'A operadora trata os seus dados pessoais em conformidade com a Lei Geral de '
            || 'Proteção de Dados (Lei nº 13.709/2018). Saiba mais sobre como cuidamos das '
            || 'suas informações em nossa política de privacidade.',
        'INFORMATIVE',
        2,
        true
    );
