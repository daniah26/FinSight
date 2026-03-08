package com.finsight.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    private Long id;
    
    @NotBlank(message = "Subscription name is required")
    @Size(max = 100, message = "Subscription name must not exceed 100 characters")
    private String merchant;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least $0.01")
    @DecimalMax(value = "10000.00", message = "Amount cannot exceed $10,000")
    private BigDecimal avgAmount;
    
    @NotNull(message = "Last paid date is required")
    @PastOrPresent(message = "Last paid date cannot be in the future")
    private LocalDate lastPaidDate;
    
    @NotNull(message = "Next due date is required")
    @FutureOrPresent(message = "Next due date cannot be in the past")
    private LocalDate nextDueDate;
    
    private String status;
    private LocalDateTime createdAt;
}
