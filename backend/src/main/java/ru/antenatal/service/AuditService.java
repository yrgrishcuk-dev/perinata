package ru.antenatal.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.antenatal.domain.AuditLog;
import ru.antenatal.repo.AuditLogRepository;
import ru.antenatal.security.CurrentPrincipal;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String actorType, String actorId, String action,
                    String entityType, String entityId, String metadata) {
        AuditLog log = new AuditLog();
        log.setActorType(actorType);
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(metadata);
        log.setIp(currentIp());
        auditLogRepository.save(log);
    }

    public void log(CurrentPrincipal principal, String action,
                    String entityType, String entityId, String metadata) {
        if (principal == null) {
            log("SYSTEM", null, action, entityType, entityId, metadata);
            return;
        }
        String actorId = principal.externalId() != null
                ? principal.externalId()
                : String.valueOf(principal.principalId());
        log(principal.type().name(), actorId, action, entityType, entityId, metadata);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
