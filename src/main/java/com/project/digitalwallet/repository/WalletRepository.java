package com.project.digitalwallet.repository;

import com.project.digitalwallet.common.enums.WalletStatus;
import com.project.digitalwallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByUserPhoneNumber(String phoneNumber);
    // Inside WalletRepository.java
    long countByStatus(WalletStatus status);

    @Query("SELECT SUM(w.balance) FROM Wallet w")
    Optional<BigDecimal> findTotalSystemBalance();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    /**
     * Issues SELECT ... FOR UPDATE on the wallet by the associated User ID.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
