package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.NotificationOutbox;
import ru.antenatal.domain.NotificationRecipientType;
import ru.antenatal.domain.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    List<NotificationOutbox> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(
            NotificationStatus status, Instant now);

    boolean existsByDedupeKey(String dedupeKey);

    Optional<NotificationOutbox> findByDedupeKey(String dedupeKey);

    void deleteByRecipientTypeAndRecipientId(NotificationRecipientType recipientType, Long recipientId);
}
