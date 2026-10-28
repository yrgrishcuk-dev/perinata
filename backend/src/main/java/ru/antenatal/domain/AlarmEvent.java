package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alarm_event")
public class AlarmEvent extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlarmType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlarmStatus status = AlarmStatus.NEW;

    private Long ackedBy;

    private Instant ackedAt;

    @Column(name = "doctor_comment", columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
