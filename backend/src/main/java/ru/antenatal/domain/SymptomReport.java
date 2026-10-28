package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "symptom_report")
public class SymptomReport extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Column(nullable = false)
    private Instant reportedAt = Instant.now();

    @Column(nullable = false)
    private boolean anyPositive;

    @Column(nullable = false, length = 1000)
    private String symptoms = "";
}
