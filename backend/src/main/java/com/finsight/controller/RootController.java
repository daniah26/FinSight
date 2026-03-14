package com.finsight.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "FinSight Backend API");
        response.put("status", "running");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.of(
            "health", "/actuator/health",
            "auth", "/api/auth/*",
            "transactions", "/api/transactions",
            "subscriptions", "/api/subscriptions",
            "dashboard", "/api/summary",
            "fraud", "/api/fraud/*"
        ));
        return ResponseEntity.ok(response);
    }
}