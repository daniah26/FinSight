package com.finsight.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_user_time", columnList = "user_id,timestamp"),
    @Index(name = "idx_audit_logs_entity", columnList = "entity_type,entity_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 50)
    private String action; // CREATE_TRANSACTION, RESOLVE_ALERT, etc.
    
    @Column(nullable = false, length = 50)
    private String entityType; // TRANSACTION, FRAUD_ALERT, SUBSCRIPTION
    
    private Long entityId;
    
    @Column(columnDefinition = "TEXT")
    private String details; // JSON format
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
