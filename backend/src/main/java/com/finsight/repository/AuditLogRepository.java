package com.finsight.repository;

import com.finsight.model.AuditLog;
import com.finsight.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserOrderByTimestampDesc(User user);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
