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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "diary_entry")
public class DiaryEntry extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Column(nullable = false)
    private LocalDate entryDate;

    private Integer sbp;

    private Integer dbp;

    private Integer pulse;

    @Column(precision = 5, scale = 1)
    private BigDecimal weightKg;

    @Column(precision = 4, scale = 1)
    private BigDecimal glucoseMmol;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GlucoseContext glucoseContext;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
