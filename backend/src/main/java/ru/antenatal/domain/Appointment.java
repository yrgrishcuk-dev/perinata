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
@Table(name = "appointment")
public class Appointment extends BaseEntity {

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private Long patientCardId;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppointmentType type = AppointmentType.VISIT;

    @Column(length = 255)
    private String place;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PatientResponse patientResponse = PatientResponse.PENDING;

    private Instant reminderSentAt;

    @Column(nullable = false)
    private int sequence = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Instant endAt() {
        return startAt.plusSeconds(durationMinutes * 60L);
    }
}
