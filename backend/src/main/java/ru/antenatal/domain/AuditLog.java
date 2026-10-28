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
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    @Column(nullable = false, length = 16)
    private String actorType;

    @Column(length = 64)
    private String actorId;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 64)
    private String entityType;

    @Column(length = 64)
    private String entityId;

    @Column(columnDefinition = "text")
    private String metadata;

    @Column(length = 64)
    private String ip;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
