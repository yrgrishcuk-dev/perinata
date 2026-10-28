package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_threshold")
public class PatientThreshold extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal value;
}
