package com.finsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String location;
    private LocalDateTime transactionDate;
    private boolean fraudulent;
    private Double fraudScore;
    private String riskLevel;
    private String status;
    private List<String> fraudReasons;
}
