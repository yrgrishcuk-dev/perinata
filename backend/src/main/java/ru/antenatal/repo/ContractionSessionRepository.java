package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.ContractionSession;

import java.util.List;
import java.util.Optional;

public interface ContractionSessionRepository extends JpaRepository<ContractionSession, Long> {
    Optional<ContractionSession> findFirstByPatientCardIdAndEndedAtIsNullOrderByStartedAtDesc(Long patientCardId);

    List<ContractionSession> findTop20ByPatientCardIdOrderByStartedAtDesc(Long patientCardId);
}
