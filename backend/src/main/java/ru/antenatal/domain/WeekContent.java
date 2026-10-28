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
@Table(name = "week_content")
public class WeekContent extends BaseEntity {

    @Column(nullable = false)
    private int weekFrom;

    @Column(nullable = false)
    private int weekTo;

    @Column(nullable = false, length = 64)
    private String version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String fetalText;

    @Column(nullable = false, columnDefinition = "text")
    private String nutritionText;

    @Column(nullable = false, columnDefinition = "text")
    private String warningText;
}
