package com.finsight.repository;

import com.finsight.model.FraudAlert;
import com.finsight.model.RiskLevel;
import com.finsight.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserOrderByCreatedAtDesc(User user);
    
    List<FraudAlert> findByUserAndResolvedOrderByCreatedAtDesc(User user, boolean resolved);
    
    List<FraudAlert> findByUserAndSeverityOrderByCreatedAtDesc(User user, RiskLevel severity);
    
    List<FraudAlert> findByUserAndResolvedAndSeverityOrderByCreatedAtDesc(User user, boolean resolved, RiskLevel severity);
}
