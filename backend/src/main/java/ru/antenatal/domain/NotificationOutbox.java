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
@Table(name = "notification_outbox")
public class NotificationOutbox extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationRecipientType recipientType;

    private Long recipientId;

    @Column(length = 64)
    private String chatId;

    @Column(name = "message_text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(unique = true, length = 255)
    private String dedupeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant sentAt;

    @Column(columnDefinition = "text")
    private String error;
}
