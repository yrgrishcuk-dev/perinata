package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.ThresholdDefault;

import java.util.Optional;

public interface ThresholdDefaultRepository extends JpaRepository<ThresholdDefault, Long> {
    Optional<ThresholdDefault> findByCode(String code);
}
