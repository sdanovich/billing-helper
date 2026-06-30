-- Schema for the platform-stack backend-login module (per-user accounts + refresh tokens).
-- The module ships the JPA entities; the consuming app owns the schema (this migration).

create table users (
    id            uuid         primary key,
    email         varchar(255) not null unique,
    password_hash varchar(255),                -- null for social (Google/GitHub) accounts
    provider      varchar(255) not null,       -- LOCAL | GOOGLE | GITHUB
    provider_id   varchar(255),
    created_at    timestamptz  not null
);

create table refresh_tokens (
    id         uuid         primary key,
    user_id    uuid         not null,
    token_hash varchar(255) not null unique,   -- SHA-256 of the raw token; raw is never stored
    created_at timestamptz  not null,
    expires_at timestamptz  not null,
    revoked    boolean      not null default false
);

create index idx_refresh_tokens_user on refresh_tokens (user_id);
