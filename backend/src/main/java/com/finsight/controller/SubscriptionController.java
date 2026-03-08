package com.finsight.controller;

import com.finsight.dto.SubscriptionDto;
import com.finsight.model.Subscription;
import com.finsight.model.SubscriptionStatus;
import com.finsight.model.User;
import com.finsight.repository.SubscriptionRepository;
import com.finsight.service.SubscriptionDetectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        // Get existing subscriptions instead of re-detecting every time
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        List<SubscriptionDto> dtos = subscriptions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/detect")
    public ResponseEntity<List<SubscriptionDto>> detectSubscriptions(@RequestParam Long userId) {
        // Separate endpoint for detecting/refreshing subscriptions
        List<Subscription> subscriptions = subscriptionDetectorService.detectSubscriptions(userId);
        List<SubscriptionDto> dtos = subscriptions.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping
    public ResponseEntity<SubscriptionDto> createSubscription(@Valid @RequestBody SubscriptionDto dto, @RequestParam Long userId) {
        // Additional validation for date range (25-35 days)
        if (dto.getLastPaidDate() != null && dto.getNextDueDate() != null) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dto.getLastPaidDate(), dto.getNextDueDate());
            
            if (daysBetween < 25) {
                throw new RuntimeException("Next due date must be at least 25 days after last paid date");
            }
            if (daysBetween > 35) {
                throw new RuntimeException("Next due date must be at most 35 days after last paid date");
            }
        }
        
        // Manually create a subscription
        User user = new User();
        user.setId(userId);
        
        Subscription subscription = Subscription.builder()
            .user(user)
            .merchant(dto.getMerchant())
            .avgAmount(dto.getAvgAmount())
            .lastPaidDate(dto.getLastPaidDate())
            .nextDueDate(dto.getNextDueDate())
            .status(SubscriptionStatus.ACTIVE)
            .createdAt(java.time.LocalDateTime.now())
            .build();
        
        subscription = subscriptionRepository.save(subscription);
        return ResponseEntity.ok(toDto(subscription));
    }
    
    @GetMapping("/due-soon")
    public ResponseEntity<List<SubscriptionDto>> getDueSoon(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "7") int days) {
        
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days);
        
        List<Subscription> subscriptions = subscriptionRepository.findDueSoonByUserId(userId, start, end);
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
