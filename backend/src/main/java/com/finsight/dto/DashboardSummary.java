package com.finsight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal currentBalance;
    private Long totalFlaggedTransactions;
    private Double averageFraudScore;
    private Map<String, BigDecimal> spendingByCategory;
    private Map<String, Long> fraudByCategory;
    private List<TimeSeriesPoint> spendingTrends;
}
