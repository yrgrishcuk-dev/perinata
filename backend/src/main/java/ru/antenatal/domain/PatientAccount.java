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
@Table(name = "patient_account")
public class PatientAccount extends BaseEntity {

    @Column(nullable = false)
    private Long patientCardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Column(nullable = false, length = 64)
    private String platformUserId;

    @Column(length = 64)
    private String chatId;

    @Column(nullable = false, length = 64)
    private String consentVersion;

    @Column(nullable = false)
    private Instant consentAt = Instant.now();

    @Column(nullable = false)
    private Instant boundAt = Instant.now();

    private Instant revokedAt;
}
