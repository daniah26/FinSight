package com.finsight.service;

import com.finsight.dto.FraudAlertDto;
import com.finsight.dto.TransactionResponse;
import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertService {
    
    private final FraudAlertRepository fraudAlertRepository;
    private final UserRepository userRepository;
    
    /**
     * Retrieves fraud alerts with optional filtering.
     */
    public List<FraudAlertDto> findByUser(Long userId, Boolean resolved, String severity) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        List<FraudAlert> alerts;
        
        if (resolved != null && severity != null) {
            RiskLevel riskLevel = RiskLevel.valueOf(severity);
            alerts = fraudAlertRepository.findByUserAndResolvedAndSeverityOrderByCreatedAtDesc(
                user, resolved, riskLevel);
        } else if (resolved != null) {
            alerts = fraudAlertRepository.findByUserAndResolvedOrderByCreatedAtDesc(user, resolved);
        } else if (severity != null) {
            RiskLevel riskLevel = RiskLevel.valueOf(severity);
            alerts = fraudAlertRepository.findByUserAndSeverityOrderByCreatedAtDesc(user, riskLevel);
        } else {
            alerts = fraudAlertRepository.findByUserOrderByCreatedAtDesc(user);
        }
        
        return alerts.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Resolves a fraud alert.
     */
    @Transactional
    public FraudAlertDto resolveAlert(Long alertId, Long userId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
            .orElseThrow(() -> new RuntimeException("Fraud alert not found: " + alertId));
        
        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to fraud alert");
        }
        
        alert.setResolved(true);
        alert = fraudAlertRepository.save(alert);
        
        log.info("Resolved fraud alert {} for user {}", alertId, userId);
        
        return toDto(alert);
    }
    
    private FraudAlertDto toDto(FraudAlert alert) {
        TransactionResponse transactionResponse = TransactionResponse.builder()
            .id(alert.getTransaction().getId())
            .amount(alert.getTransaction().getAmount())
            .type(alert.getTransaction().getType())
            .category(alert.getTransaction().getCategory())
            .description(alert.getTransaction().getDescription())
            .location(alert.getTransaction().getLocation())
            .transactionDate(alert.getTransaction().getTransactionDate())
            .fraudulent(alert.getTransaction().isFraudulent())
            .fraudScore(alert.getTransaction().getFraudScore())
            .riskLevel(alert.getSeverity().name())
            .status("FLAGGED")
            .build();
        
        return FraudAlertDto.builder()
            .id(alert.getId())
            .userId(alert.getUser().getId())
            .transaction(transactionResponse)
            .message(alert.getMessage())
            .severity(alert.getSeverity().name())
            .resolved(alert.isResolved())
            .createdAt(alert.getCreatedAt())
            .build();
    }
}
