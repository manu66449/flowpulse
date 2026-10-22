package com.flowpulse.core.service;

import com.flowpulse.core.domain.AuditLog;
import com.flowpulse.core.domain.User;
import com.flowpulse.core.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an audit log entry.
     * Uses REQUIRES_NEW propagation to ensure audit logs are saved
     * even if the outer business transaction rolls back (optional, but great for security trails).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityName, Long entityId, String action, User performedBy, String oldState, String newState) {
        AuditLog auditLog = AuditLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .oldState(oldState)
                .newState(newState)
                .build();

        auditLogRepository.save(auditLog);
    }
}