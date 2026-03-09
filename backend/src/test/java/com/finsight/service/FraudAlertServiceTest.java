package com.finsight.service;

import com.finsight.dto.FraudAlertDto;
import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.Transaction;
import com.finsight.model.User;
import com.finsight.repository.FraudAlertRepository;
import com.finsight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudAlertServiceTest {

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FraudAlertService fraudAlertService;

    private User testUser;
    private Transaction testTransaction;
    private FraudAlert testAlert;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .build();

        testTransaction = Transaction.builder()
            .id(1L)
            .user(testUser)
            .amount(BigDecimal.valueOf(5000))
            .type("EXPENSE")
            .category("luxury")
            .fraudulent(true)
            .fraudScore(85.0)
            .transactionDate(LocalDateTime.now())
            .build();

        testAlert = FraudAlert.builder()
            .id(1L)
            .user(testUser)
            .transaction(testTransaction)
            .message("Suspicious transaction detected")
            .severity(RiskLevel.HIGH)
            .resolved(false)
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    void findByUser_NoFilters_ReturnsAllAlerts() {
        // Given
        List<FraudAlert> alerts = Arrays.asList(
            testAlert,
            FraudAlert.builder()
                .id(2L)
                .user(testUser)
                .transaction(testTransaction)
                .message("Another alert")
                .severity(RiskLevel.MEDIUM)
                .resolved(false)
                .createdAt(LocalDateTime.now())
                .build()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserOrderByCreatedAtDesc(testUser))
            .thenReturn(alerts);

        // When
        List<FraudAlertDto> responses = fraudAlertService.findByUser(1L, null, null);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getMessage()).isEqualTo("Suspicious transaction detected");
        assertThat(responses.get(1).getMessage()).isEqualTo("Another alert");
    }

    @Test
    void findByUser_WithResolvedFilter_ReturnsFilteredAlerts() {
        // Given
        FraudAlert unresolvedAlert = FraudAlert.builder()
            .id(1L)
            .user(testUser)
            .transaction(testTransaction)
            .message("Unresolved alert")
            .severity(RiskLevel.HIGH)
            .resolved(false)
            .createdAt(LocalDateTime.now())
            .build();

        List<FraudAlert> unresolvedAlerts = List.of(unresolvedAlert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserAndResolvedOrderByCreatedAtDesc(testUser, false))
            .thenReturn(unresolvedAlerts);

        // When
        List<FraudAlertDto> responses = fraudAlertService.findByUser(1L, false, null);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isResolved()).isFalse();
        assertThat(responses.get(0).getMessage()).isEqualTo("Unresolved alert");
    }

    @Test
    void findByUser_WithSeverityFilter_ReturnsFilteredAlerts() {
        // Given
        List<FraudAlert> highSeverityAlerts = List.of(testAlert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserAndSeverityOrderByCreatedAtDesc(testUser, RiskLevel.HIGH))
            .thenReturn(highSeverityAlerts);

        // When
        List<FraudAlertDto> responses = fraudAlertService.findByUser(1L, null, "HIGH");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void findByUser_WithBothFilters_ReturnsFilteredAlerts() {
        // Given
        List<FraudAlert> filteredAlerts = List.of(testAlert);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserAndResolvedAndSeverityOrderByCreatedAtDesc(
            testUser, false, RiskLevel.HIGH))
            .thenReturn(filteredAlerts);

        // When
        List<FraudAlertDto> responses = fraudAlertService.findByUser(1L, false, "HIGH");

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isResolved()).isFalse();
        assertThat(responses.get(0).getSeverity()).isEqualTo("HIGH");
    }

    @Test
    void findByUser_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.findByUser(999L, null, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void resolveAlert_ExistingAlert_ResolvesSuccessfully() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // When
        FraudAlertDto response = fraudAlertService.resolveAlert(1L, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(fraudAlertRepository).save(any(FraudAlert.class));
        assertThat(testAlert.isResolved()).isTrue();
    }

    @Test
    void resolveAlert_AlertNotFound_ThrowsException() {
        // Given
        when(fraudAlertRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.resolveAlert(999L, 1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Fraud alert not found");
    }

    @Test
    void resolveAlert_UnauthorizedUser_ThrowsException() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));

        // When/Then
        assertThatThrownBy(() -> fraudAlertService.resolveAlert(1L, 999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unauthorized access");
    }

    @Test
    void resolveAlert_AlreadyResolved_StillSucceeds() {
        // Given
        testAlert.setResolved(true);
        
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // When
        FraudAlertDto response = fraudAlertService.resolveAlert(1L, 1L);

        // Then
        assertThat(response.isResolved()).isTrue();
        verify(fraudAlertRepository).save(testAlert);
    }

    @Test
    void toDto_MapsCorrectly() {
        // Given
        when(fraudAlertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(fraudAlertRepository.save(any(FraudAlert.class))).thenReturn(testAlert);

        // When
        FraudAlertDto dto = fraudAlertService.resolveAlert(1L, 1L);

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getMessage()).isEqualTo("Suspicious transaction detected");
        assertThat(dto.getSeverity()).isEqualTo("HIGH");
        assertThat(dto.getTransaction()).isNotNull();
        assertThat(dto.getTransaction().getId()).isEqualTo(1L);
    }

    @Test
    void findByUser_ReturnsMultipleAlerts() {
        // Given
        List<FraudAlert> alerts = Arrays.asList(
            testAlert,
            FraudAlert.builder().id(2L).user(testUser).transaction(testTransaction).message("Alert 2").severity(RiskLevel.MEDIUM).resolved(false).createdAt(LocalDateTime.now()).build(),
            FraudAlert.builder().id(3L).user(testUser).transaction(testTransaction).message("Alert 3").severity(RiskLevel.LOW).resolved(false).createdAt(LocalDateTime.now()).build()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(fraudAlertRepository.findByUserOrderByCreatedAtDesc(testUser))
            .thenReturn(alerts);

        // When
        List<FraudAlertDto> responses = fraudAlertService.findByUser(1L, null, null);

        // Then
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
    }
}
