package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ration_plan")
public class RationPlan extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BmiCategory bmiCategory;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int kcal;

    @Column(name = "protein_g", nullable = false)
    private int proteinG;

    @Column(name = "fat_g", nullable = false)
    private int fatG;

    @Column(name = "carbs_g", nullable = false)
    private int carbsG;

    @Column(nullable = false, columnDefinition = "text")
    private String meals;
}
