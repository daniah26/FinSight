package com.finsight.service;

import com.finsight.dto.FraudDetectionResult;
import com.finsight.dto.TransactionRequest;
import com.finsight.dto.TransactionResponse;
import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
import com.finsight.repository.TransactionRepository;
import com.finsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditLogService auditLogService;
    private final FraudAlertRepository fraudAlertRepository;
    
    /**
     * Creates a manual transaction with fraud detection.
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
        
        Transaction transaction = Transaction.builder()
            .user(user)
            .amount(request.getAmount())
            .type(request.getType())
            .category(request.getCategory())
            .description(request.getDescription())
            .location(request.getLocation())
            .transactionDate(request.getTransactionDate())
            .createdAt(LocalDateTime.now())
            .fraudulent(false) // Default to false
            .fraudScore(0.0)   // Default to 0
            .build();
        
        // Save transaction FIRST so it's included in fraud detection calculations
        transaction = transactionRepository.save(transaction);
        
        // Run fraud detection AFTER saving
        FraudDetectionResult fraudResult = fraudDetectionService.analyzeTransaction(transaction);
        
        // Update transaction with fraud detection results
        transaction.setFraudulent(fraudResult.isFraudulent());
        transaction.setFraudScore(fraudResult.getFraudScore());
        transaction = transactionRepository.save(transaction);
        
        // Create fraud alert if ANY rule triggered (score > 0)
        if (fraudResult.getFraudScore() > 0 && !fraudResult.getReasons().isEmpty()) {
            createFraudAlert(transaction, fraudResult);
            log.info("Created fraud alert for transaction {} with score {} ({})", 
                transaction.getId(), fraudResult.getFraudScore(), fraudResult.getRiskLevel());
        }
        
        // Log action
        auditLogService.logAction(user.getId(), "CREATE_TRANSACTION", "TRANSACTION", 
            transaction.getId(), String.format("{\"amount\": %s, \"category\": \"%s\", \"fraudScore\": %.2f}", 
                transaction.getAmount(), transaction.getCategory(), fraudResult.getFraudScore()));
        
        log.info("Created transaction {} for user {} with fraud score {} (Risk: {})", 
            transaction.getId(), user.getId(), fraudResult.getFraudScore(), fraudResult.getRiskLevel());
        
        return toResponse(transaction, fraudResult.getRiskLevel());
    }
    
    /**
     * Retrieves transactions with filtering, sorting, and pagination.
     */
    public Page<TransactionResponse> findWithFilters(Long userId, String type, String category,
                                                     LocalDateTime startDate, LocalDateTime endDate,
                                                     Boolean fraudulent, String sortBy, String sortDir,
                                                     int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Specification<Transaction> spec = Specification.where(null);
        
        // User filter (always applied)
        spec = spec.and((root, query, cb) -> cb.equal(root.get("user"), user));
        
        // Type filter
        if (type != null && !type.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        
        // Category filter
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        
        // Date range filter
        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.between(root.get("transactionDate"), startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
        } else if (endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
        }
        
        // Fraudulent filter
        if (fraudulent != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("fraudulent"), fraudulent));
        }
        
        // Sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"), 
                           sortBy != null ? sortBy : "transactionDate");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);
        
        return transactions.map(t -> toResponse(t, RiskLevel.fromScore(t.getFraudScore() != null ? t.getFraudScore() : 0.0)));
    }
    
    private void createFraudAlert(Transaction transaction, FraudDetectionResult fraudResult) {
        FraudAlert alert = FraudAlert.builder()
            .user(transaction.getUser())
            .transaction(transaction)
            .message(String.format("Suspicious transaction detected: %s", 
                String.join(", ", fraudResult.getReasons())))
            .severity(fraudResult.getRiskLevel())
            .resolved(false)
            .createdAt(LocalDateTime.now())
            .build();
        
        fraudAlertRepository.save(alert);
        log.warn("Created fraud alert for transaction {}", transaction.getId());
    }
    
    private TransactionResponse toResponse(Transaction transaction, RiskLevel riskLevel) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .category(transaction.getCategory())
            .description(transaction.getDescription())
            .location(transaction.getLocation())
            .transactionDate(transaction.getTransactionDate())
            .fraudulent(transaction.isFraudulent())
            .fraudScore(transaction.getFraudScore())
            .riskLevel(riskLevel != null ? riskLevel.name() : "LOW")
            .status(transaction.isFraudulent() ? "FLAGGED" : "COMPLETED")
            .build();
    }
}
