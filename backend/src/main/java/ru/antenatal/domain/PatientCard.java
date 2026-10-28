package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_card")
public class PatientCard extends BaseEntity {

    @Column(nullable = false)
    private Long clinicId;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false, unique = true, length = 16)
    private String publicCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private int ageYears;

    @Column(name = "parity")
    private Integer parity;

    @Column(nullable = false)
    private LocalDate edd;

    @Column(precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(precision = 5, scale = 1)
    private BigDecimal prePregnancyWeightKg;

    @Column(precision = 5, scale = 1)
    private BigDecimal bmi;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private BmiCategory bmiCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private RiskGroup riskGroup = RiskGroup.LOW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DiabetesType diabetesType = DiabetesType.NONE;

    @Column(nullable = false)
    private boolean diaryRequired;

    @Column(nullable = false)
    private LocalTime reminderTime = LocalTime.of(9, 0);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PatientStatus status = PatientStatus.ACTIVE;

    @Column(length = 64)
    private String contentVersion;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant closedAt;
}
