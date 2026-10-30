alter table appointment add column sequence integer not null default 0;

create table calendar_feed_token
(
    id         bigserial primary key,
    owner_type varchar(16) not null,
    owner_id   bigint      not null,
    token_hash varchar(64) not null unique,
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);

create unique index ux_calendar_feed_token_active
    on calendar_feed_token (owner_type, owner_id)
    where revoked_at is null;
