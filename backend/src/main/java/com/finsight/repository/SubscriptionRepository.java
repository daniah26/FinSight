package com.finsight.repository;

import com.finsight.model.Subscription;
import com.finsight.model.SubscriptionStatus;
import com.finsight.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUser(User user);
    
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId")
    List<Subscription> findByUserId(@Param("userId") Long userId);
    
    List<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.user = :user " +
           "AND s.status = 'ACTIVE' " +
           "AND s.nextDueDate BETWEEN :start AND :end")
    List<Subscription> findDueSoon(@Param("user") User user, 
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
    
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId " +
           "AND s.status = 'ACTIVE' " +
           "AND s.nextDueDate BETWEEN :start AND :end")
    List<Subscription> findDueSoonByUserId(@Param("userId") Long userId, 
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);
}
