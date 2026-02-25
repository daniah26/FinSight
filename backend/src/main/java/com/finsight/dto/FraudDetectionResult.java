package com.finsight.dto;

import com.finsight.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionResult {
    private boolean fraudulent;
    private double fraudScore; // 0-100
    private RiskLevel riskLevel;
    
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
}
