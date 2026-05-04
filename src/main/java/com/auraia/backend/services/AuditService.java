package com.auraia.backend.services;

import com.auraia.backend.models.entities.AuditLog;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.repositories.AuditLogRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, String action, String entityType, String entityId, Map<String, Object> metadata) {
        auditLogRepository.save(AuditLog.builder()
            .actorUser(actor)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .metadata(metadata == null ? new LinkedHashMap<>() : metadata)
            .build());
    }
}
