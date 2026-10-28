alter table patient_card add column parity int;

alter table patient_card
    add constraint ck_patient_card_parity
        check (parity is null or parity between 1 and 20);
