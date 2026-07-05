-- SPEC-0004: product-wide notifications — one mechanism turning domain events into in-app items
-- (bell + notification center) and, per user preference, e-mails (BR5/BR6/BR7).
--
-- Three tables:
--   * notification_event_type — the registry catalog (BR5: reference data, not an enum,
--     baseline §0019): code, description (pt-BR label for the preferences screen), email_default
--     (does this type e-mail by default) and mandatory (security/account types that cannot be
--     e-mail-disabled, BR7).
--   * notification — one in-app item per delivery, scoped to an account (BR1/BR3); read state is
--     the nullability of read_at.
--   * notification_preference — the per-account e-mail opt-out per event type (BR7).

create table notification_event_type (
    code varchar(64) primary key,
    description varchar(200) not null,
    email_default boolean not null,
    mandatory boolean not null
);

create table notification (
    id uuid primary key,
    account_id uuid not null,
    event_type_code varchar(64) not null references notification_event_type (code),
    title varchar(120) not null,
    body varchar(500) not null,
    link varchar(500),
    created_at timestamptz not null,
    read_at timestamptz
);

-- Newest-first, account-scoped listing (BR3) and the unread count (BR2) both hit this index.
create index idx_notification_account_created_at on notification (account_id, created_at desc);

create table notification_preference (
    account_id uuid not null,
    event_type_code varchar(64) not null references notification_event_type (code),
    email_opt_out boolean not null default false,
    primary key (account_id, event_type_code)
);

-- BR5 catalog seed (codes referenced across specs; descriptions are the pt-BR preferences-screen
-- labels). Mandatory security/account types (BR7) cannot be e-mail-disabled. DL-0008: the two
-- account types whose e-mail the existing identity listeners already send keep email_default=false
-- so this module never double-sends; account.contact-changed and the business types e-mail by
-- default. Producers for account.locked, account.contact-changed and the business types are wired
-- by their own specs/integration — this phase only wires account.password-changed and seeds the
-- full catalog + the generic mechanism.
insert into notification_event_type (code, description, email_default, mandatory) values
    ('account.password-changed', 'Senha alterada', false, true),
    ('account.locked', 'Conta bloqueada', false, true),
    ('account.contact-changed', 'Dados de contato alterados', true, true),
    ('reimbursement.paid', 'Reembolso pago', true, false),
    ('guide.status-changed', 'Guia com mudança de status', true, false),
    ('appointment.confirmed', 'Consulta confirmada', true, false);
