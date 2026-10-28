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
@Table(name = "doctor_messenger")
public class DoctorMessenger extends BaseEntity {

    @Column(nullable = false)
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Column(nullable = false, length = 64)
    private String platformUserId;

    @Column(nullable = false, length = 64)
    private String chatId;

    @Column(nullable = false)
    private Instant linkedAt = Instant.now();

    private Instant revokedAt;
}
