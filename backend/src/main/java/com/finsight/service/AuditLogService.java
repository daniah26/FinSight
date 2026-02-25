package com.finsight.service;

import com.finsight.model.AuditLog;
import com.finsight.model.User;
import com.finsight.repository.AuditLogRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    /**
     * Logs a user action for compliance tracking.
     * 
     * @param userId The user performing the action
     * @param action Action type
     * @param entityType Entity being acted upon
     * @param entityId ID of the entity
     * @param details Additional details in JSON format
     */
    @Transactional
    public void logAction(Long userId, String action, String entityType, 
                         Long entityId, String details) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        AuditLog auditLog = AuditLog.builder()
            .user(user)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .details(details)
            .timestamp(LocalDateTime.now(ZoneOffset.UTC))
            .build();
        
        auditLogRepository.save(auditLog);
        
        log.info("Audit log created: user={}, action={}, entityType={}, entityId={}", 
            userId, action, entityType, entityId);
    }
}
