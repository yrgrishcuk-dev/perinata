package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.MessageThread;

import java.util.Optional;

public interface MessageThreadRepository extends JpaRepository<MessageThread, Long> {
    Optional<MessageThread> findByPatientCardId(Long patientCardId);
}
