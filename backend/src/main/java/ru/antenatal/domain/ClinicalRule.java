package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinical_rule")
public class ClinicalRule extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(nullable = false, columnDefinition = "text")
    private String params;

    @Column(nullable = false)
    private boolean active = true;
}
