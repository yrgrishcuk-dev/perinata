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
@Table(name = "contraction_session")
public class ContractionSession extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant endedAt;
}
