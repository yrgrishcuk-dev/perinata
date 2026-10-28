package ru.antenatal.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.antenatal.domain.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByCreatedAtDesc();

    List<AuditLog> findTop200ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
}
