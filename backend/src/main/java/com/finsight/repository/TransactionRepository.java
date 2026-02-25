package com.finsight.repository;

import com.finsight.model.Transaction;
import com.finsight.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, 
                                              JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByUserOrderByTransactionDateDesc(User user);
    
    Optional<Transaction> findTopByUserOrderByTransactionDateDesc(User user);
    
    List<Transaction> findByUserAndFraudulentTrue(User user);
    
    List<Transaction> findByUserAndTransactionDateAfter(User user, LocalDateTime after);
    
    Long countByUser(User user);
    
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.user = :user")
    BigDecimal calculateAverageAmount(@Param("user") User user);
    
    @Query("SELECT DISTINCT t.category FROM Transaction t WHERE t.user = :user")
    List<String> findDistinctCategoriesByUser(@Param("user") User user);
    
    Page<Transaction> findByUser(User user, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.user = :user " +
           "AND t.transactionDate BETWEEN :start AND :end")
    long countByUserAndTransactionDateBetween(@Param("user") User user, 
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
    
    List<Transaction> findByUserAndType(User user, String type);
    
    List<Transaction> findByUserAndTransactionDateBetween(User user, 
                                                          LocalDateTime start, 
                                                          LocalDateTime end);
}
