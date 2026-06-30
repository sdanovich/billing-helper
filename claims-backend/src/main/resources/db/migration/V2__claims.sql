-- Claims system of record + scanned document images. Images are a SEPARATE table so the
-- structured-claims read path (which the agent reads read-only over the MCP tunnel) never
-- selects image bytes.

create table claim_images (
    id           uuid        primary key,
    content      bytea       not null,
    content_type varchar(100),
    filename     varchar(255),
    created_at   timestamptz not null
);

create table claims (
    claim_id       varchar(64)    primary key,
    patient_name   varchar(255)   not null,
    member_id      varchar(64)    not null,
    payer          varchar(255)   not null,
    cpt_code       varchar(16),
    icd_code       varchar(16),
    billed_amount  numeric(12, 2) not null,
    paid_amount    numeric(12, 2) not null,
    balance        numeric(12, 2) not null,
    status         varchar(16)    not null,   -- submitted | paid | denied | pending
    denial_reason  varchar(500),              -- only when status = denied
    image_id       uuid           references claim_images (id),
    created_at     timestamptz    not null
);

create index idx_claims_status on claims (status);
create index idx_claims_payer on claims (payer);
create index idx_claims_member on claims (member_id);
