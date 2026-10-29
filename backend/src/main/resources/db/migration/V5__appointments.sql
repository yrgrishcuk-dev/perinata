create table appointment
(
    id               bigserial primary key,
    doctor_id        bigint       not null references doctor (id) on delete cascade,
    patient_card_id  bigint       not null references patient_card (id) on delete cascade,
    start_at         timestamptz  not null,
    duration_minutes integer      not null default 30,
    type             varchar(32)  not null default 'VISIT',
    place            varchar(255),
    comment          text,
    status           varchar(32)  not null default 'SCHEDULED',
    patient_response varchar(32)  not null default 'PENDING',
    reminder_sent_at timestamptz,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now()
);

create index ix_appointment_doctor_start on appointment (doctor_id, start_at);
create index ix_appointment_patient_start on appointment (patient_card_id, start_at);
create index ix_appointment_reminder on appointment (status, reminder_sent_at, start_at);
