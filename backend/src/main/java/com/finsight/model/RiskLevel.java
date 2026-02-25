package com.finsight.model;

public enum RiskLevel {
    LOW,    // Score 0-39
    MEDIUM, // Score 40-69
    HIGH;   // Score 70-100
    
    public static RiskLevel fromScore(double score) {
        if (score >= 70) {
            return HIGH;
        } else if (score >= 40) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }
}
