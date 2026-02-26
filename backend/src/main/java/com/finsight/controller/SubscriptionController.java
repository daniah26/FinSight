package com.finsight.controller;

import com.finsight.dto.SubscriptionDto;
import com.finsight.model.Subscription;
import com.finsight.model.SubscriptionStatus;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.service.SubscriptionDetectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    
    private final SubscriptionDetectorService subscriptionDetectorService;
    private final SubscriptionRepository subscriptionRepository;
    
    @GetMapping
    public ResponseEntity<List<SubscriptionDto>> getSubscriptions(@RequestParam Long userId) {
        List<Subscription> subscriptions = subscriptionDetectorService.detectSubscriptions(userId);
        List<SubscriptionDto> dtos = subscriptions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/due-soon")
    public ResponseEntity<List<SubscriptionDto>> getDueSoon(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "7") int days) {
        
        List<Subscription> subscriptions = subscriptionDetectorService.findDueSoon(userId, days);
        List<SubscriptionDto> dtos = subscriptions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PutMapping("/{id}/ignore")
    public ResponseEntity<SubscriptionDto> ignoreSubscription(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
        
        subscription.setStatus(SubscriptionStatus.IGNORED);
        subscription = subscriptionRepository.save(subscription);
        
        return ResponseEntity.ok(toDto(subscription));
    }
    
    private SubscriptionDto toDto(Subscription subscription) {
        return SubscriptionDto.builder()
            .id(subscription.getId())
            .merchant(subscription.getMerchant())
            .avgAmount(subscription.getAvgAmount())
            .lastPaidDate(subscription.getLastPaidDate())
            .nextDueDate(subscription.getNextDueDate())
            .status(subscription.getStatus().name())
            .createdAt(subscription.getCreatedAt())
            .build();
    }
}
