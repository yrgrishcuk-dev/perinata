alter table diary_entry
    add column discharge_character varchar(16),
    add column discharge_amount varchar(16),
    add column discharge_odor boolean,
    add column vaginal_ph numeric(3, 1)
        check (vaginal_ph is null or vaginal_ph between 3.0 and 7.0),
    add column fetal_movements varchar(16),
    add column lower_abdominal_pain varchar(16),
    add column pain_intensity int
        check (pain_intensity is null or pain_intensity between 0 and 10),
    add column leg_edema varchar(16);

alter table patient_card
    add column edd_method varchar(16) not null default 'MANUAL',
    add column edd_basis_date date,
    add column edd_cycle_length int
        check (edd_cycle_length is null or edd_cycle_length between 21 and 35),
    add column edd_calculated_at timestamptz;

update patient_card set edd_method = 'MANUAL' where edd_method is null;
