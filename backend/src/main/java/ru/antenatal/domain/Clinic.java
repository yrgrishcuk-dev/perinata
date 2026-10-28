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
@Table(name = "clinic")
public class Clinic extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 64)
    private String timezone = "Europe/Moscow";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
