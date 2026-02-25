package com.finsight.controller;

import com.finsight.dto.TransactionRequest;
import com.finsight.dto.TransactionResponse;
import com.finsight.service.DemoDataService;
import com.finsight.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    private final DemoDataService demoDataService;
    
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean fraudulent,
            @RequestParam(required = false, defaultValue = "transactionDate") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        Page<TransactionResponse> transactions = transactionService.findWithFilters(
            userId, type, category, startDate, endDate, fraudulent, sortBy, sortDir, page, size);
        
        return ResponseEntity.ok(transactions);
    }
    
    @PostMapping("/reseed-demo")
    public ResponseEntity<Map<String, Object>> reseedDemoData(@RequestParam Long userId) {
        try {
            int count = demoDataService.forceReseedUser(userId);
            return ResponseEntity.ok(Map.of(
                "message", "Demo data reseeded successfully",
                "transactionsCreated", count
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Failed to reseed demo data: " + e.getMessage()
            ));
        }
    }
}
