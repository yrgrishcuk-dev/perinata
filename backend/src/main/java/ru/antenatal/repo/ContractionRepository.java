package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.Contraction;

import java.time.Instant;
import java.util.List;

public interface ContractionRepository extends JpaRepository<Contraction, Long> {
    List<Contraction> findBySessionIdOrderByStartAtAsc(Long sessionId);

    List<Contraction> findBySessionIdAndStartAtGreaterThanEqualOrderByStartAtAsc(Long sessionId, Instant from);
}
