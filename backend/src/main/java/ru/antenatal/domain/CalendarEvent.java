package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "calendar_event")
public class CalendarEvent extends BaseEntity {

    @Column(nullable = false)
    private Long doctorId;

    private Long patientCardId;

    @Column(nullable = false)
    private LocalDate eventDate;

    private LocalTime eventTime;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String note;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
