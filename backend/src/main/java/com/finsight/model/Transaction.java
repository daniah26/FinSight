package com.finsight.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_user_date", columnList = "user_id,transaction_date"),
    @Index(name = "idx_transactions_fraudulent", columnList = "user_id,fraudulent"),
    @Index(name = "idx_transactions_category", columnList = "user_id,category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 20)
    private String type; // INCOME, EXPENSE
    
    @Column(nullable = false, length = 50)
    private String category;
    
    @Column(length = 255)
    private String description;
    
    @Column(length = 100)
    private String location;
    
    @Column(nullable = false)
    private LocalDateTime transactionDate;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean fraudulent = false;
    
    @Column
    private Double fraudScore; // 0-100
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
