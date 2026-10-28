package ru.antenatal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "telegram_update")
public class TelegramUpdate {

    @Id
    private Long updateId;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();
}
