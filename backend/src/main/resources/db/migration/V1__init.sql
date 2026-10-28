create table clinic
(
    id         bigserial primary key,
    name       varchar(255) not null,
    timezone   varchar(64)  not null default 'Europe/Moscow',
    created_at timestamptz  not null default now()
);

create table doctor
(
    id                   bigserial primary key,
    clinic_id            bigint       not null references clinic (id),
    full_name            varchar(255) not null,
    email                varchar(255) not null unique,
    password_hash        varchar(255) not null,
    doctor_role          varchar(16)  not null,
    status               varchar(16)  not null default 'ACTIVE',
    must_change_password boolean      not null default false,
    created_at           timestamptz  not null default now(),
    last_login_at        timestamptz
);

create table doctor_messenger
(
    id               bigserial primary key,
    doctor_id        bigint      not null references doctor (id) on delete cascade,
    platform         varchar(16) not null,
    platform_user_id varchar(64) not null,
    chat_id          varchar(64) not null,
    linked_at        timestamptz not null default now(),
    revoked_at       timestamptz
);

create unique index ux_doctor_messenger_active
    on doctor_messenger (platform, platform_user_id) where revoked_at is null;

create table auth_session
(
    id             bigserial primary key,
    token_hash     varchar(64) not null unique,
    principal_type varchar(16) not null,
    principal_id   varchar(64) not null,
    created_at     timestamptz not null default now(),
    expires_at     timestamptz not null
);

create index ix_auth_session_expires on auth_session (expires_at);

create table patient_card
(
    id                        bigserial primary key,
    clinic_id                 bigint       not null references clinic (id),
    doctor_id                 bigint       not null references doctor (id),
    public_code               varchar(16)  not null unique,
    full_name                 varchar(255) not null,
    age_years                 int          not null check (age_years between 12 and 60),
    edd                       date         not null,
    height_cm                 numeric(5, 1) check (height_cm is null or height_cm between 100 and 250),
    pre_pregnancy_weight_kg   numeric(5, 1) check (pre_pregnancy_weight_kg is null or pre_pregnancy_weight_kg between 30 and 250),
    bmi                       numeric(5, 1) check (bmi is null or bmi between 5 and 100),
    bmi_category              varchar(16),
    risk_group                varchar(8)   not null default 'LOW',
    diabetes_type             varchar(16)  not null default 'NONE',
    diary_required            boolean      not null default false,
    reminder_time             time         not null default '09:00',
    status                    varchar(16)  not null default 'ACTIVE',
    content_version           varchar(64),
    created_at                timestamptz  not null default now(),
    closed_at                 timestamptz
);

create index ix_patient_card_doctor on patient_card (doctor_id, status);
create index ix_patient_card_clinic on patient_card (clinic_id, status);

create table patient_account
(
    id               bigserial primary key,
    patient_card_id  bigint      not null references patient_card (id) on delete cascade,
    platform         varchar(16) not null,
    platform_user_id varchar(64) not null,
    chat_id          varchar(64),
    consent_version  varchar(64) not null,
    consent_at       timestamptz not null,
    bound_at         timestamptz not null default now(),
    revoked_at       timestamptz
);

create unique index ux_patient_account_active
    on patient_account (platform, platform_user_id) where revoked_at is null;
create unique index ux_patient_account_card_active
    on patient_account (patient_card_id) where revoked_at is null;

create table link_token
(
    id              bigserial primary key,
    token_hash      varchar(64) not null unique,
    patient_card_id bigint      not null references patient_card (id) on delete cascade,
    created_by      bigint      not null references doctor (id),
    expires_at      timestamptz not null,
    used_at         timestamptz,
    revoked_at      timestamptz,
    created_at      timestamptz not null default now()
);

create index ix_link_token_card on link_token (patient_card_id, created_at desc);

create table doctor_link_token
(
    id         bigserial primary key,
    token_hash varchar(64) not null unique,
    doctor_id  bigint      not null references doctor (id) on delete cascade,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);

create table diary_entry
(
    id              bigserial primary key,
    patient_card_id bigint      not null references patient_card (id) on delete cascade,
    entry_date      date        not null,
    sbp             int check (sbp is null or sbp between 60 and 250),
    dbp             int check (dbp is null or dbp between 30 and 150),
    pulse           int check (pulse is null or pulse between 30 and 220),
    weight_kg       numeric(5, 1) check (weight_kg is null or weight_kg between 30 and 250),
    glucose_mmol    numeric(4, 1) check (glucose_mmol is null or glucose_mmol between 2 and 30),
    glucose_context varchar(16),
    note            text,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),
    unique (patient_card_id, entry_date),
    check (sbp is null or dbp is null or sbp > dbp)
);

create index ix_diary_patient_date on diary_entry (patient_card_id, entry_date desc);

create table symptom_report
(
    id              bigserial primary key,
    patient_card_id bigint       not null references patient_card (id) on delete cascade,
    reported_at     timestamptz  not null default now(),
    any_positive    boolean      not null,
    symptoms        varchar(1000) not null default ''
);

create index ix_symptom_patient on symptom_report (patient_card_id, reported_at desc);

create table alarm_event
(
    id              bigserial primary key,
    patient_card_id bigint      not null references patient_card (id) on delete cascade,
    type            varchar(16) not null,
    severity        varchar(16) not null,
    payload         text,
    status          varchar(16) not null default 'NEW',
    acked_by        bigint references doctor (id),
    acked_at        timestamptz,
    doctor_comment  text,
    created_at      timestamptz not null default now()
);

create index ix_alarm_patient_created on alarm_event (patient_card_id, created_at desc);
create index ix_alarm_status on alarm_event (status, created_at desc);

create table contraction_session
(
    id              bigserial primary key,
    patient_card_id bigint      not null references patient_card (id) on delete cascade,
    started_at      timestamptz not null default now(),
    ended_at        timestamptz
);

create index ix_contraction_session_patient on contraction_session (patient_card_id, started_at desc);

create table contraction
(
    id               bigserial primary key,
    session_id       bigint      not null references contraction_session (id) on delete cascade,
    start_at         timestamptz not null,
    end_at           timestamptz not null,
    duration_seconds int         not null check (duration_seconds > 0),
    check (end_at > start_at)
);

create index ix_contraction_session on contraction (session_id, start_at);

create table week_content
(
    id             bigserial primary key,
    week_from      int          not null,
    week_to        int          not null,
    version        varchar(64)  not null,
    title          varchar(255) not null,
    fetal_text     text         not null,
    nutrition_text text         not null,
    warning_text   text         not null
);

create index ix_week_content_week on week_content (week_from, week_to);

create table ration_plan
(
    id           bigserial primary key,
    bmi_category varchar(16)  not null,
    version      varchar(64)  not null,
    title        varchar(255) not null,
    kcal         int          not null,
    protein_g    int          not null,
    fat_g        int          not null,
    carbs_g      int          not null,
    meals        text         not null
);

create index ix_ration_category on ration_plan (bmi_category);

create table gain_corridor
(
    id           bigserial primary key,
    bmi_category varchar(16)   not null,
    week         int           not null,
    min_gain_kg  numeric(5, 2) not null,
    max_gain_kg  numeric(5, 2) not null,
    version      varchar(64)   not null,
    unique (bmi_category, week, version)
);

create table clinical_rule
(
    id      bigserial primary key,
    code    varchar(64) not null,
    version varchar(64) not null,
    params  text        not null,
    active  boolean     not null default true
);

create index ix_clinical_rule_code on clinical_rule (code, active);

create table threshold_default
(
    id       bigserial primary key,
    code     varchar(64)   not null unique,
    value    numeric(6, 2) not null,
    severity varchar(16)   not null,
    version  varchar(64)   not null
);

create table patient_threshold
(
    id              bigserial primary key,
    patient_card_id bigint        not null references patient_card (id) on delete cascade,
    code            varchar(64)   not null,
    value           numeric(6, 2) not null,
    unique (patient_card_id, code)
);

create table message_thread
(
    id              bigserial primary key,
    patient_card_id bigint      not null unique references patient_card (id) on delete cascade,
    created_at      timestamptz not null default now()
);

create table message
(
    id          bigserial primary key,
    thread_id   bigint      not null references message_thread (id) on delete cascade,
    direction   varchar(16) not null,
    author_type varchar(16) not null,
    author_id   bigint,
    body        text        not null,
    created_at  timestamptz not null default now(),
    read_at     timestamptz
);

create index ix_message_thread on message (thread_id, created_at);

create table notification_outbox
(
    id             bigserial primary key,
    platform       varchar(16)  not null,
    recipient_type varchar(16)  not null,
    recipient_id   bigint,
    chat_id        varchar(64),
    message_text   text         not null,
    dedupe_key     varchar(255) unique,
    status         varchar(16)  not null default 'PENDING',
    attempts       int          not null default 0,
    next_attempt_at timestamptz not null default now(),
    created_at     timestamptz  not null default now(),
    sent_at        timestamptz,
    error          text
);

create index ix_outbox_pending on notification_outbox (status, next_attempt_at);

create table audit_log
(
    id          bigserial primary key,
    actor_type  varchar(16) not null,
    actor_id    varchar(64),
    action      varchar(64) not null,
    entity_type varchar(64),
    entity_id   varchar(64),
    metadata    text,
    ip          varchar(64),
    created_at  timestamptz not null default now()
);

create index ix_audit_created on audit_log (created_at desc);
create index ix_audit_entity on audit_log (entity_type, entity_id, created_at desc);

create table app_setting
(
    setting_key   varchar(128) primary key,
    setting_value text         not null
);

create table content_release
(
    version     varchar(64) primary key,
    approved_by varchar(255),
    approved_at timestamptz
);

create table telegram_update
(
    update_id   bigint primary key,
    received_at timestamptz not null default now()
);
