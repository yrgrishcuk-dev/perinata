package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.AlarmEvent;
import ru.antenatal.domain.AlarmStatus;
import ru.antenatal.domain.AlarmType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlarmEventRepository extends JpaRepository<AlarmEvent, Long> {
    List<AlarmEvent> findByPatientCardIdOrderByCreatedAtDesc(Long patientCardId);

    List<AlarmEvent> findByStatusOrderByCreatedAtDesc(AlarmStatus status);

    List<AlarmEvent> findByStatusInOrderByCreatedAtDesc(Collection<AlarmStatus> statuses);

    Optional<AlarmEvent> findFirstByPatientCardIdAndTypeAndStatusInAndCreatedAtAfterOrderByCreatedAtDesc(
            Long patientCardId, AlarmType type, Collection<AlarmStatus> statuses, Instant after);

    long countByStatus(AlarmStatus status);

    long countByPatientCardIdAndStatus(Long patientCardId, AlarmStatus status);
}
