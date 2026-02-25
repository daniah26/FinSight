package com.finsight.controller;

import com.finsight.dto.FraudAlertDto;
import com.finsight.service.FraudAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud/alerts")
@RequiredArgsConstructor
public class FraudAlertController {
    
    private final FraudAlertService fraudAlertService;
    
    @GetMapping
    public ResponseEntity<List<FraudAlertDto>> getAlerts(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) String severity) {
        
        List<FraudAlertDto> alerts = fraudAlertService.findByUser(userId, resolved, severity);
        return ResponseEntity.ok(alerts);
    }
    
    @PutMapping("/{id}/resolve")
    public ResponseEntity<FraudAlertDto> resolveAlert(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        FraudAlertDto alert = fraudAlertService.resolveAlert(id, userId);
        return ResponseEntity.ok(alert);
    }
}
