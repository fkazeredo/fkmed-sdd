-- Embedded Spring Authorization Server + Spring Session JDBC state (DECISIONS-BASELINE
-- §0018/§0020): client registrations, authorization state and the form-login session live in
-- the database so the app stays multi-instance ready. Flyway owns this schema
-- (docs/architecture/persistence.md); initialize-schema is disabled in application.yaml.
-- Column types follow the official Spring schemas, with text for the JSON/token columns on PostgreSQL (Security 7 writes them as strings).

create table oauth2_registered_client (
    id varchar(100) not null,
    client_id varchar(100) not null,
    client_id_issued_at timestamp default current_timestamp not null,
    client_secret varchar(200) default null,
    client_secret_expires_at timestamp default null,
    client_name varchar(200) not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types varchar(1000) not null,
    redirect_uris varchar(1000) default null,
    post_logout_redirect_uris varchar(1000) default null,
    scopes varchar(1000) not null,
    client_settings varchar(2000) not null,
    token_settings varchar(2000) not null,
    primary key (id)
);

create table oauth2_authorization (
    id varchar(100) not null,
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorization_grant_type varchar(100) not null,
    authorized_scopes varchar(1000) default null,
    attributes text default null,
    state varchar(500) default null,
    authorization_code_value text default null,
    authorization_code_issued_at timestamp default null,
    authorization_code_expires_at timestamp default null,
    authorization_code_metadata text default null,
    access_token_value text default null,
    access_token_issued_at timestamp default null,
    access_token_expires_at timestamp default null,
    access_token_metadata text default null,
    access_token_type varchar(100) default null,
    access_token_scopes varchar(1000) default null,
    oidc_id_token_value text default null,
    oidc_id_token_issued_at timestamp default null,
    oidc_id_token_expires_at timestamp default null,
    oidc_id_token_metadata text default null,
    refresh_token_value text default null,
    refresh_token_issued_at timestamp default null,
    refresh_token_expires_at timestamp default null,
    refresh_token_metadata text default null,
    user_code_value text default null,
    user_code_issued_at timestamp default null,
    user_code_expires_at timestamp default null,
    user_code_metadata text default null,
    device_code_value text default null,
    device_code_issued_at timestamp default null,
    device_code_expires_at timestamp default null,
    device_code_metadata text default null,
    primary key (id)
);

create table oauth2_authorization_consent (
    registered_client_id varchar(100) not null,
    principal_name varchar(200) not null,
    authorities varchar(1000) not null,
    primary key (registered_client_id, principal_name)
);

-- Spring Session JDBC (official schema-postgresql.sql)
create table spring_session (
    primary_id char(36) not null,
    session_id char(36) not null,
    creation_time bigint not null,
    last_access_time bigint not null,
    max_inactive_interval int not null,
    expiry_time bigint not null,
    principal_name varchar(100),
    constraint spring_session_pk primary key (primary_id)
);

create unique index spring_session_ix1 on spring_session (session_id);
create index spring_session_ix2 on spring_session (expiry_time);
create index spring_session_ix3 on spring_session (principal_name);

create table spring_session_attributes (
    session_primary_id char(36) not null,
    attribute_name varchar(200) not null,
    attribute_bytes bytea not null,
    constraint spring_session_attributes_pk primary key (session_primary_id, attribute_name),
    constraint spring_session_attributes_fk foreign key (session_primary_id)
        references spring_session (primary_id) on delete cascade
);
