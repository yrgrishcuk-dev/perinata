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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gain_corridor")
public class GainCorridor extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BmiCategory bmiCategory;

    @Column(nullable = false)
    private int week;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal minGainKg;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxGainKg;

    @Column(nullable = false, length = 64)
    private String version;
}
