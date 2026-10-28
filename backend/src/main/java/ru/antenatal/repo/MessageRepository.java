package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.Message;
import ru.antenatal.domain.MessageDirection;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    long countByThreadIdAndDirectionAndReadAtIsNull(Long threadId, MessageDirection direction);
}
