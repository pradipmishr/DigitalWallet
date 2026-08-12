package com.project.digitalwallet.repository;

import com.project.digitalwallet.entity.Transaction;
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
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findBySenderWalletId(Long walletId);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    List<Transaction> findByReceiverWalletId(Long walletId);

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByWalletId(@Param("walletId") Long walletId, Pageable pageable);

    // Sum transferred today
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.senderWallet.id = :walletId " +
            "AND t.type = 'TRANSFER' " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :startOfDay AND t.createdAt <= :endOfDay")
    BigDecimal findTotalTransferredToday(
            @Param("walletId") Long walletId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // Sum transferred this month (NEW)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.senderWallet.id = :walletId " +
            "AND t.type = 'TRANSFER' " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :startOfMonth AND t.createdAt <= :endOfMonth")
    BigDecimal findTotalTransferredThisMonth(
            @Param("walletId") Long walletId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.senderWallet.id = :walletId " +
            "AND t.type IN ('TRANSFER') " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :startOfDay AND t.createdAt <= :endOfDay")
    BigDecimal findTotalOutgoingToday(
            @Param("walletId") Long walletId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT t FROM Transaction t WHERE (t.senderWallet.user.id = :userId OR t.receiverWallet.user.id = :userId) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.senderWallet.user.id = :userId OR t.receiverWallet.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);
}