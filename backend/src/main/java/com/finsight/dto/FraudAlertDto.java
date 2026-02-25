package com.finsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertDto {
    private Long id;
    private Long userId;
    private TransactionResponse transaction;
    private String message;
    private String severity;
    private boolean resolved;
    private LocalDateTime createdAt;
}
