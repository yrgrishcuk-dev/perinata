alter table diary_entry
    add column kick_started_at timestamptz,
    add column kick_ended_at timestamptz,
    add column kick_count int not null default 0;
