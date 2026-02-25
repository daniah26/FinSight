package com.finsight.dto;

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
    private String merchant;
    private BigDecimal avgAmount;
    private LocalDate lastPaidDate;
    private LocalDate nextDueDate;
    private String status;
    private LocalDateTime createdAt;
}
