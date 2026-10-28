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
@Table(name = "message_thread")
public class MessageThread extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long patientCardId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
